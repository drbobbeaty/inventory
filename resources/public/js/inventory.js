/*
 * Function to load up the data at the start of the operation from the
 * back-end system into the page. This starts things off just fine.
 */
function buildTable() {
  console.log("attempting to load the data");
  // make the call to pull the data with column and row headers
  $.getJSON("/v1/cars", function(data) {
    // get the components we need from the returned JSON
    $("#mainTable").handsontable({ data: data.data,
                                   colHeaders: data.colHeaders,
                                   stretchH: 'all',
                                   contextMenu: false,
                                   manualColumnResize: true,
                                   rowHeaders: data.rowHeaders });
  });
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
  var body = { colHeaders: $('#mainTable').handsontable('getColHeader'),
               rowHeaders: $('#mainTable').handsontable('getRowHeader'),
               data: $('#mainTable').handsontable('getData') };
  // now, make the POST to update the data
  $.ajax({type: "POST",
          url: "/v1/cars",
          processData: false,
          contentType: 'application/json',
          data: JSON.stringify(body),
          success: function(resp) {
            var cont = '<div class="alert alert-success" role="alert">';
            cont += '<strong>Saved!</strong>';
            cont += '</div>';
            $("#status").replaceWith(cont);
          }
  });
}