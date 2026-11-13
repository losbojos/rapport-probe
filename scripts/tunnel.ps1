# SSH tunnel to prod monitoring on the Jitsi VPS
ssh -L 8081:127.0.0.1:8081 -L 9090:127.0.0.1:9090 -L 3001:127.0.0.1:3001 root@195.19.195.228