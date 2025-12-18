# Keycloak Authorization Server

## Export configuration

See [Keycloak documentation](https://www.keycloak.org/server/importExport#_exporting_a_specific_realm) for more details.
```shell
docker compose exec keycloak /opt/keycloak/bin/kc.sh export --file /opt/keycloak/data/import/lyse-tele.json --realm lyse-tele --users realm_file
```
