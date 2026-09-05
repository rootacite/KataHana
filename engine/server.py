#!/usr/bin/env python3
"""Wrap KataGo analysis as a WebSocket JSON service for KataHana."""

from __future__ import annotations

import argparse
import asyncio
import json
import os
import shutil
import signal
import subprocess
import sys
import threading
import urllib.error
import urllib.request
from pathlib import Path

ENGINE_DIR = Path(__file__).resolve().parent
ROOT = ENGINE_DIR.parent
MODEL_DIR = ROOT / "model"
DEFAULT_CONFIG = ENGINE_DIR / "analysis.cfg"
DEFAULT_HOME = ENGINE_DIR / "home"
DEFAULT_BIN = ENGINE_DIR / "bin" / "katago"

MAIN_NAME = "b10c384h6nbttflrs.bin.gz"
HUMAN_NAME = "b18c384nbt-humanv0.bin.gz"
MAIN_URL = f"https://github.com/lightvector/KataGo/releases/download/v1.17.0/{MAIN_NAME}"
HUMAN_URL = f"https://github.com/lightvector/KataGo/releases/download/v1.15.0/{HUMAN_NAME}"

# Tuned 2026-09-05: i7-14700F + RX 9070 XT, KataGo 1.18.1 ROCm, b10c384h6nbttflrs.
TUNED_ANALYSIS_THREADS = 2
TUNED_SEARCH_THREADS = 20
TUNED_NN_MAX_BATCH_SIZE = 40
TUNED_NN_SERVER_THREADS = 1

try:
    from websockets.asyncio.server import ServerConnection, serve
    from websockets.exceptions import ConnectionClosed
except ImportError:
    print(
        "missing dependency 'websockets'. Install with:\n"
        "  pip install -r engine/requirements.txt",
        file=sys.stderr,
    )
    raise SystemExit(1)


class KataGoWSServer:
    def __init__(self, cmd: list[str]):
        self.cmd = cmd
        self.proc: subprocess.Popen[str] | None = None
        self.ready = threading.Event()
        self._stdin_lock = asyncio.Lock()
        self._id_to_ws: dict[str, ServerConnection] = {}
        self._ws_ids: dict[ServerConnection, set[str]] = {}
        self._clients: set[ServerConnection] = set()
        self._loop: asyncio.AbstractEventLoop | None = None

    def start_engine(self) -> None:
        print("Starting KataGo:", " ".join(self.cmd), flush=True)
        self.proc = subprocess.Popen(
            self.cmd,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            bufsize=1,
        )
        threading.Thread(target=self._stderr_loop, name="katago-stderr", daemon=True).start()
        if not self.ready.wait(timeout=120):
            raise RuntimeError("KataGo did not become ready within 120s")
        if self.proc.poll() is not None:
            raise RuntimeError(f"KataGo exited early with code {self.proc.returncode}")

    def _stderr_loop(self) -> None:
        assert self.proc and self.proc.stderr
        for line in self.proc.stderr:
            text = line.rstrip()
            print(text, file=sys.stderr, flush=True)
            if "ready to begin handling requests" in text:
                self.ready.set()
        if not self.ready.is_set():
            self.ready.set()

    async def _stdout_loop(self) -> None:
        assert self.proc and self.proc.stdout
        loop = asyncio.get_running_loop()
        while True:
            line = await loop.run_in_executor(None, self.proc.stdout.readline)
            if not line:
                print("KataGo stdout closed", file=sys.stderr, flush=True)
                break
            payload = line.rstrip("\n")
            if not payload:
                continue
            await self._dispatch(payload)

    async def _dispatch(self, payload: str) -> None:
        target: ServerConnection | None = None
        try:
            data = json.loads(payload)
            qid = data.get("id")
            if isinstance(qid, str):
                target = self._id_to_ws.get(qid)
        except json.JSONDecodeError:
            pass
        if target is not None:
            await self._send(target, payload)
            return
        dead = []
        for ws in list(self._clients):
            if not await self._send(ws, payload):
                dead.append(ws)
        for ws in dead:
            self._drop_client(ws)

    async def _send(self, ws: ServerConnection, payload: str) -> bool:
        try:
            await ws.send(payload)
            return True
        except Exception:
            return False

    def _drop_client(self, ws: ServerConnection) -> None:
        self._clients.discard(ws)
        for qid in self._ws_ids.pop(ws, set()):
            if self._id_to_ws.get(qid) is ws:
                self._id_to_ws.pop(qid, None)

    async def handler(self, ws: ServerConnection) -> None:
        peer = ws.remote_address
        print(f"client connected: {peer}", flush=True)
        self._clients.add(ws)
        self._ws_ids.setdefault(ws, set())
        try:
            async for message in ws:
                if isinstance(message, bytes):
                    message = message.decode("utf-8")
                text = message.strip()
                if not text:
                    continue
                try:
                    data = json.loads(text)
                except json.JSONDecodeError:
                    print(f"ignoring non-JSON from {peer}: {text[:120]}", file=sys.stderr, flush=True)
                    continue
                qid = data.get("id")
                if isinstance(qid, str):
                    self._id_to_ws[qid] = ws
                    self._ws_ids[ws].add(qid)
                await self._write_engine(text)
        except ConnectionClosed:
            pass
        finally:
            self._drop_client(ws)
            print(f"client disconnected: {peer}", flush=True)

    async def _write_engine(self, text: str) -> None:
        assert self.proc and self.proc.stdin
        if not text.endswith("\n"):
            text += "\n"
        async with self._stdin_lock:
            self.proc.stdin.write(text)
            self.proc.stdin.flush()

    async def run(self, host: str, port: int) -> None:
        self._loop = asyncio.get_running_loop()
        stdout_task = asyncio.create_task(self._stdout_loop())
        print(f"KataGo analysis WS listening on ws://{host}:{port}", flush=True)
        async with serve(self.handler, host, port):
            stop = asyncio.Future()
            for sig in (signal.SIGINT, signal.SIGTERM):
                self._loop.add_signal_handler(sig, stop.set_result, None)
            await stop
        stdout_task.cancel()

    def shutdown(self) -> None:
        proc = self.proc
        if proc is None:
            return
        if proc.poll() is None:
            try:
                if proc.stdin:
                    proc.stdin.close()
            except Exception:
                pass
            proc.send_signal(signal.SIGTERM)
            try:
                proc.wait(timeout=5)
            except subprocess.TimeoutExpired:
                proc.kill()


def looks_like_gzip(path: Path) -> bool:
    try:
        with path.open("rb") as fh:
            return fh.read(2) == b"\x1f\x8b"
    except OSError:
        return False


def model_ready(path: Path) -> bool:
    return path.is_file() and path.stat().st_size > 1_000_000 and looks_like_gzip(path)


def download(url: str, dest: Path) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    part = dest.with_suffix(dest.suffix + ".part")
    if part.exists():
        part.unlink()
    print(f"downloading {dest.name} …", flush=True)
    request = urllib.request.Request(url, headers={"User-Agent": "KataHana-engine"})
    with urllib.request.urlopen(request, timeout=120) as resp, part.open("wb") as out:
        total = int(resp.headers.get("Content-Length") or 0)
        got = 0
        while True:
            chunk = resp.read(1024 * 256)
            if not chunk:
                break
            out.write(chunk)
            got += len(chunk)
            if total > 0:
                pct = min(100.0, 100.0 * got / total)
                print(
                    f"\r  {dest.name}  {pct:5.1f}%  {got / 1e6:.1f}/{total / 1e6:.1f} MB",
                    end="",
                    file=sys.stderr,
                    flush=True,
                )
            else:
                print(f"\r  {dest.name}  {got / 1e6:.1f} MB", end="", file=sys.stderr, flush=True)
    print(file=sys.stderr, flush=True)
    if not looks_like_gzip(part) or part.stat().st_size < 1_000_000:
        part.unlink(missing_ok=True)
        raise RuntimeError(f"download did not look like a KataGo net: {url}")
    part.replace(dest)
    print(f"saved {dest}", flush=True)


def ensure_model(path: Path, url: str, required: bool) -> bool:
    if model_ready(path):
        print(f"using {path}", flush=True)
        return True
    try:
        download(url, path)
    except (OSError, RuntimeError, urllib.error.URLError) as exc:
        if required:
            raise RuntimeError(f"failed to fetch required model {path.name}: {exc}") from exc
        print(f"human model unavailable ({exc}); Human SL disabled", file=sys.stderr, flush=True)
        return False
    if not model_ready(path):
        if required:
            raise RuntimeError(f"model missing after download: {path}")
        return False
    return True


def resolve_katago(explicit: str | None) -> Path:
    if explicit:
        path = Path(explicit).expanduser()
        if path.is_file() and os.access(path, os.X_OK):
            return path
        raise RuntimeError(f"katago not found or not executable: {path}")
    if DEFAULT_BIN.is_file() and os.access(DEFAULT_BIN, os.X_OK):
        return DEFAULT_BIN
    which = shutil.which("katago")
    if which:
        return Path(which)
    raise RuntimeError(
        "katago binary not found. Pass --katago /path/to/katago "
        f"or place it at {DEFAULT_BIN}"
    )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="KataGo analysis WebSocket server")
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--port", type=int, default=2080)
    parser.add_argument("--katago", default=None, help=f"KataGo binary (default: {DEFAULT_BIN} or PATH)")
    parser.add_argument("--model", default=str(MODEL_DIR / MAIN_NAME))
    parser.add_argument("--human-model", default=str(MODEL_DIR / HUMAN_NAME))
    parser.add_argument("--config", default=str(DEFAULT_CONFIG))
    parser.add_argument("--home", default=str(DEFAULT_HOME))
    parser.add_argument("--skip-download", action="store_true", help="do not fetch missing nets")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        katago = resolve_katago(args.katago)
    except RuntimeError as exc:
        print(exc, file=sys.stderr)
        return 1
    config = Path(args.config)
    if not config.is_file():
        print(f"config not found: {config}", file=sys.stderr)
        return 1

    model = Path(args.model)
    human = Path(args.human_model) if args.human_model else None
    if not args.skip_download:
        try:
            ensure_model(model, MAIN_URL, required=True)
            if human is not None:
                if not ensure_model(human, HUMAN_URL, required=False):
                    human = None
        except RuntimeError as exc:
            print(exc, file=sys.stderr)
            return 1
    elif not model_ready(model):
        print(f"model not found: {model}", file=sys.stderr)
        return 1

    os.makedirs(args.home, exist_ok=True)
    overrides = ",".join(
        [
            f"homeDataDir={args.home}",
            f"numAnalysisThreads={TUNED_ANALYSIS_THREADS}",
            f"numSearchThreads={TUNED_SEARCH_THREADS}",
            f"nnMaxBatchSize={TUNED_NN_MAX_BATCH_SIZE}",
            f"numNNServerThreadsPerModel={TUNED_NN_SERVER_THREADS}",
            "rocmDeviceToUse=0",
        ]
    )
    cmd = [str(katago), "analysis", "-model", str(model)]
    if human is not None and model_ready(human):
        cmd += ["-human-model", str(human)]
    elif args.human_model:
        print(f"human model not found, Human SL disabled: {args.human_model}", file=sys.stderr)
    cmd += ["-config", str(config), "-override-config", overrides]
    server = KataGoWSServer(cmd)
    try:
        server.start_engine()
        asyncio.run(server.run(args.host, args.port))
    except KeyboardInterrupt:
        pass
    finally:
        server.shutdown()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
