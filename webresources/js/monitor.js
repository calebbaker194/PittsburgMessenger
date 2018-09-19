
window.onload = init;
function init() {
    google.charts.load('current', {'packages':['corechart']});
    google.charts.setOnLoadCallback(drawChart);

    function drawChart() {

      $.get('monitor/chartdata.json',function(jsonData) {

        var options = {
          title: 'Company Performance',
          curveType: 'function',
          legend: { position: 'bottom' },
          'backgroundColor': 'transparent'
        };


        for(var i in jsonData.charts) {
          var chart = new google.visualization.LineChart(document.getElementById(i));
          chart.draw(google.visualization.arrayToDataTable(jsonData[i]), options);
        }

      });
    }
}

function start(index) {
  $.post('start',{'index':index},function(data) {
    if(data) {
      console.log(data);
    }
  });
}

function restart(index) {
  $.post('restart',{'index':index},function(data) {
    if(data) {
      console.log(data);
    }
  });
}

function stop(index) {
  $.post('stop',{'index':index},function(data) {
    if(data) {
      console.log(data);
    }
  });
}