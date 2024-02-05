const apiUrl = 'https://localhost:4567';
 
function createSpace(name, owner) {
    let data = {name: name, owner: owner};
 
    fetch(apiUrl + '/spaces', {
        method: 'POST',
        credentials: 'include',
        body: JSON.stringify(data),
        headers: {
            'Content-Type': 'application/json',
            'Authroization': 'Bearer ' + token
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
    .catch(error => console.error('Error: ', error));}

function getCookie(cookieName) {
  var cookieValue = document.cookie.split(';')
  .map(item=>item.split('=')
    .map(x => decodeURIComponent(x.trim())))
  .filter(item=>item[0] === cookieName)[0];
  if(cookieValue){
    return cookieValue[1];
  }
}