#!/bin/sh
# Entrypoint of every dev image (`make up`, `make test`, ...).
#
# Runs the command as the user who owns the mounted source folder (the working
# directory), so files the tools write there (target/, .next/, coverage reports...)
# belong to you on Linux and WSL instead of root. If the source shows up as owned by
# root (some macOS setups), it simply stays root.
#
# When the Docker socket is mounted (make test-integration), that user also joins the
# socket's group so Testcontainers can start containers.
set -eu

if [ "$(id -u)" != 0 ]; then
  exec "$@"
fi

uid=$(stat -c %u .)
gid=$(stat -c %g .)
if [ "$uid" = 0 ]; then
  exec "$@"
fi

# Tools like Java and git need a passwd entry with a home folder.
if ! getent passwd "$uid" >/dev/null; then
  echo "dev$uid:x:$uid:$gid::/home/dev:/bin/sh" >>/etc/passwd
fi
if ! getent group "$gid" >/dev/null; then
  echo "dev$gid:x:$gid:" >>/etc/group
fi

# Caches and named volumes must be writable by that user.
for dir in "$HOME" ${DEV_WRITABLE_DIRS:-}; do
  mkdir -p "$dir"
  if [ "$(stat -c %u "$dir")" != "$uid" ]; then
    chown -R "$uid:$gid" "$dir"
  fi
done

groups=$gid
if [ -S /var/run/docker.sock ]; then
  groups="$groups,$(stat -c %g /var/run/docker.sock)"
fi

exec setpriv --reuid="$uid" --regid="$gid" --groups="$groups" -- "$@"
