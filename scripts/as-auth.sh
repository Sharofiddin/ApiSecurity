ACCESS_TOKEN=$(curl -u test:KQOWD7KS4pv9ncYP8MogEw5lM0UGnxvc http://localhost:8080/realms/test/protocol/openid-connect/token -d 'grant_type=password&scope=create_space+post_message&username=demo&password=1qazxsw2')
curl -H 'Content-Type: application/json' -H "Authorization: Bearer $ACCESS_TOKEN"  -d '{"name":"test","owner":"demo"}' https://localhost:4567/spaces
