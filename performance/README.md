# k6 performance tests

Run these commands from the project root. Set `BASE_URL` explicitly so a load
test cannot accidentally target the wrong server.

```powershell
$env:BASE_URL='https://jdsskbus.duckdns.org'
$env:TARGET_IP='192.168.219.101'
k6 run .\performance\smoke.js
k6 run .\performance\load.js
k6 run .\performance\stress.js
k6 run .\performance\complaint-api-load.js
```

`TARGET_IP` is optional. When set, only k6 resolves `jdsskbus.duckdns.org` to
that IP while keeping the HTTPS URL and hostname. Use it only when testing from
the same LAN and after reserving the server IP with DHCP. This is a LAN-path
test, not an external-internet test. For an external test, run from outside the
LAN and remove the override first:

```powershell
Remove-Item Env:TARGET_IP -ErrorAction SilentlyContinue
```

The generated JSON summaries are saved under `performance/results`.

Run `smoke.js` first and then `load.js`. Run `stress.js` only during a suitable
time window while monitoring the application, MySQL, CPU, and memory. The
stress test ramps through 10, 20, 30, and 40 VUs and aborts automatically when
the error rate reaches 2%, p95 reaches 1500 ms, or checks fall to 98%.

`complaint-api-load.js` targets the public complaint-list API only. It validates
the HTTP status and JSON paging shape while measuring the database-backed list
and count queries separately from static pages.
