/*
 * Function that's called on Google login to set the email and auth
 * token to global vars so that we can use them in all the calls to
 * the server. This is the security we'll be using.
 */
var user_auth_token = '';
function onSignIn(googleUser) {
  user_auth_token = googleUser.getAuthResponse().id_token;
  // console.log('auth token: ' + user_auth_token);
  // now let's try to get the data - as we had to have the token
  buildTable();
}

/*
 * Function to load up the data at the start of the operation from the
 * back-end system into the page. This starts things off just fine.
 */
function buildTable() {
  if (user_auth_token != '') {
    console.log("attempting to load the data");
    // make the call to pull the data with column and row headers
    $.ajax({type: "GET",
            url: "/v1/cars",
            dataType: "json",
            headers: { authorization: 'bearer ' + user_auth_token },
            success: function(data) {
              // get the components we need from the returned JSON
              $("#mainTable").handsontable({ data: data.inventory,
                                             colHeaders: data.manufacturers,
                                             stretchH: 'all',
                                             contextMenu: false,
                                             manualColumnResize: true,
                                             rowHeaders: data.model_years });
              // hide the status as we are 'fresh' from the source now
              $("#status").hide();
            }
    });
  }
}

/*
 * Function to package up the data in the table and ship it back
 * to the server so that it can be updated in the database. Very
 * simple, but quite effective.
 */
function saveData() {
  // let the user know it's being done
  console.log("attempting to save the data");
  // get the data from the table
  var body = { manufacturers: $('#mainTable').handsontable('getColHeader'),
               model_years: $('#mainTable').handsontable('getRowHeader'),
               inventory: $('#mainTable').handsontable('getData') };
  // now, make the POST to update the data
  $.ajax({type: "POST",
          url: "/v1/cars",
          processData: false,
          headers: { authorization: 'bearer ' + user_auth_token },
          contentType: 'application/json',
          data: JSON.stringify(body),
          success: function(resp) {
            if (resp.status == "OK") {
              var cont = '<div class="alert alert-success" role="alert">';
              cont += '<strong>Saved!</strong>';
              cont += '</div>';
              $("#status").html(cont).show().fadeOut(3000);
            } else {
              var cont = '<div class="alert alert-danger" role="alert">';
              cont += '<strong>Error!</strong>';
              cont += '</div>';
              $("#status").html(cont).show().fadeOut(5000);
            }
          }
  });
}