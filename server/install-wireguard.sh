#!/usr/bin/env bash
set -euo pipefail

WG_DIR=/etc/wireguard
SERVER_ADDR="10.66.66.1/24"
CLIENT_ADDR="10.66.66.2/32"
PORT=51820
WAN_IF=$(ip route show default | awk '/default/ {print $5; exit}')

apt-get update
DEBIAN_FRONTEND=noninteractive apt-get install -y wireguard qrencode iptables curl
mkdir -p "$WG_DIR"
chmod 700 "$WG_DIR"
umask 077

SERVER_PRIV=$(wg genkey)
SERVER_PUB=$(printf '%s' "$SERVER_PRIV" | wg pubkey)
CLIENT_PRIV=$(wg genkey)
CLIENT_PUB=$(printf '%s' "$CLIENT_PRIV" | wg pubkey)

cat > "$WG_DIR/wg0.conf" <<WGCONF
[Interface]
Address = ${SERVER_ADDR}
ListenPort = ${PORT}
PrivateKey = ${SERVER_PRIV}
PostUp = iptables -A FORWARD -i %i -j ACCEPT; iptables -A FORWARD -o %i -j ACCEPT; iptables -t nat -A POSTROUTING -o ${WAN_IF} -j MASQUERADE
PostDown = iptables -D FORWARD -i %i -j ACCEPT; iptables -D FORWARD -o %i -j ACCEPT; iptables -t nat -D POSTROUTING -o ${WAN_IF} -j MASQUERADE

[Peer]
PublicKey = ${CLIENT_PUB}
AllowedIPs = ${CLIENT_ADDR}
WGCONF

cat > /etc/sysctl.d/99-volgavpn.conf <<SYSCTL
net.ipv4.ip_forward=1
net.ipv6.conf.all.forwarding=1
SYSCTL
sysctl --system >/dev/null || true

ufw allow ${PORT}/udp 2>/dev/null || true
systemctl enable --now wg-quick@wg0

PUBLIC_IP=$(curl -4 -fsS https://api.ipify.org || hostname -I | awk '{print $1}')
cat > /root/volgavpn-client.conf <<CLIENTCONF
[Interface]
PrivateKey = ${CLIENT_PRIV}
Address = ${CLIENT_ADDR}
DNS = 1.1.1.1

[Peer]
PublicKey = ${SERVER_PUB}
Endpoint = ${PUBLIC_IP}:${PORT}
AllowedIPs = 0.0.0.0/0
PersistentKeepalive = 25
CLIENTCONF
chmod 600 /root/volgavpn-client.conf

echo
printf '%s\n' 'VolgaVPN WireGuard server is ready.'
printf '%s\n' 'Client config: /root/volgavpn-client.conf'
printf '%s\n' "Server public IP: ${PUBLIC_IP}"
printf '%s\n' 'Import the client config into the VolgaVPN Android app.'
