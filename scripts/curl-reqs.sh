
curl -X POST -H 'Content-Type: application/json' -d '{"username":"demo", "password":"1qazxsw2"}' https://localhost:4567/users
#create space
curl -i -u demo:1qazxsw2 -X POST -H 'Content-Type: application/json' -d '{"name":"demoSpace", "owner":"demo"}' https://localhost:4567/spaces
curl -i -H 'Authorization: Bearer' -X POST -H 'Content-Type: application/json' -d '{"name":"demoSpace", "owner":"demo"}' https://localhost:4567/spaces
#read message
curl -i -u demo:1qazxsw2 -X GET https://localhost:4567/spaces/1/messages/1

#read messages
curl -i -u demo:1qazxsw2 -X GET https://localhost:4567/spaces/1/messages

# Post message
curl -i -u demo:1qazxsw2 -X POST -H 'Content-Type: application/json' -d '{"author":"demo", "message":"hi i am demo"}' https://localhost:4567/spaces/1/messages
curl -i -u -H 'Authorization: Bearer'  -X POST -H 'Content-Type: application/json' -d '{"author":"demo", "message":"hi i am demo"}' https://localhost:4567/spaces/1/messages

# add member
curl -X POST -i -u 'demo:1qazxsw2' -H 'Content-Type: application/json' -d '{"username":"demo2", "space_id":1, "permissions":"r"}' https://localhost:4567/spaces/1/members

# delete msg
curl -i -X DELETE -u demoevil:1qazxsw2 https://localhost:4567/spaces/1/messages/1

# create session
curl -i -c cookies -u demo:1qazxsw2 -X POST  -H 'Content-Type: application/json' https://localhost:4567/sessions
curl -i  -u demo:1qazxsw2 -X POST  -H 'Content-Type: application/json' https://localhost:4567/sessions
# create space with cookie
curl -i -b cookies -X POST -H 'Content-Type: application/json' -d '{"name":"demoSpace", "owner":"demo"}' https://localhost:4567/spaces

# keycloak get acces token
curl -u test:aaSNKLtdz13Fc6C91RHJ3NCaqeg3HVCK http://localhost:8080/realms/test/protocol/openid-connect/token -d 'grant_type=password&scope=create_space+post_message&username=demo&password=1qazxsw2'

# auth with access token
curl -H 'Content-Type: application/json' -H 'Authorization: Bearer '  -d '{"name":"test","owner":"demo"}' https://localhost:4567/spaces
