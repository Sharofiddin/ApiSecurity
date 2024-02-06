const apiUrl = 'https://localhost:4567';
 
function createSpace(name, owner) {
    let data = {name: name, owner: owner};
    let token = localStorage.getItem('token');
    fetch(apiUrl + '/spaces', {
        method: 'POST',
        body: JSON.stringify(data),
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + token
        }
    })
    .then(response => {
        if (response.ok) {
            return response.json();
        } else {
            throw Error(response.statusText);
        }
    })
    .then(json => console.log('Created space: ', json.name, json.uri))
    .catch(error => console.error('Error: ', error));
}
