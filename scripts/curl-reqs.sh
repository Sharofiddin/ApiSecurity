
curl -X POST -H 'Content-Type: application/json' -d '{"username":"demo", "password":"1qazxsw2"}' https://localhost:4567/users
#create space
curl -i -u demo:1qazxsw2 -X POST -H 'Content-Type: application/json' -d '{"name":"demoSpace", "owner":"demo"}' https://localhost:4567/spaces

#read message
curl -i -u demo:1qazxsw2 -X GET https://localhost:4567/spaces/1/messages/1

#read messages
curl -i -u demo:1qazxsw2 -X GET https://localhost:4567/spaces/1/messages

# Post message
curl -i -u demo:1qazxsw2 -X POST -H 'Content-Type: application/json' -d '{"author":"demo", "message":"hi i am demo"}' https://localhost:4567/spaces/1/messages

# add member
curl -X POST -i -u 'demo:1qazxsw2' -H 'Content-Type: application/json' -d '{"username":"demo2", "space_id":1, "permissions":"r"}' https://localhost:4567/spaces/1/members

# delete msg
curl -i -X DELETE -u demoevil:1qazxsw2 https://localhost:4567/spaces/1/messages/1