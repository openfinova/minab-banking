#!/bin/sh
set -e

# Named volumes mount as root; the app runs as openfinova and must write persisted keys.
if [ -d /var/lib/openfinova/tan ]; then
  chown -R openfinova:openfinova /var/lib/openfinova/tan
fi

exec su -s /bin/sh openfinova -c 'exec java -jar /app/app.jar'
