# TLS certificates

Place the production certificate chain and private key here:

- `fullchain.pem`
- `privkey.pem`

The private key must remain outside Git and must be readable only by the deployment operator.

The production Nginx configuration intentionally refuses plain HTTP by redirecting it to HTTPS.
