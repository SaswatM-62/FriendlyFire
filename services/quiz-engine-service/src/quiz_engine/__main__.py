"""python -m quiz_engine: run the gRPC server until SIGTERM or Ctrl+C."""

import asyncio
import logging
import signal

from quiz_engine.bootstrap import SERVICE_NAME, build_server
from quiz_engine.settings import Settings

log = logging.getLogger(SERVICE_NAME)


async def serve() -> None:
    service = await build_server(Settings())
    await service.start()
    log.info("listening on port %d", service.port)

    stop = asyncio.Event()
    loop = asyncio.get_running_loop()
    for signum in (signal.SIGTERM, signal.SIGINT):
        loop.add_signal_handler(signum, stop.set)
    await stop.wait()

    log.info("shutting down")
    await service.stop()


def main() -> None:
    logging.basicConfig(
        level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s"
    )
    asyncio.run(serve())


if __name__ == "__main__":
    main()
