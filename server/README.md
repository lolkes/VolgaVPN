# Free WireGuard server

For a zero-cost test server, use an Oracle Cloud Always Free Ubuntu VM.

1. Create an Always Free Ubuntu ARM VM.
2. Allow UDP/51820 in the cloud firewall/security list.
3. SSH to the VM.
4. Copy `install-wireguard.sh` to it and run:

```bash
sudo bash install-wireguard.sh
```

5. Copy `/root/volgavpn-client.conf` to the Android phone.
6. Import it into VolgaVPN.

Never commit `wg0.conf`, `volgavpn-client.conf`, private keys, or `.env` files to GitHub.
