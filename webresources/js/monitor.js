
window.onload = init;
function init() {
    google.charts.load('current', {'packages':['corechart']});
    google.charts.setOnLoadCallback(drawChart);

    function drawChart() {
        var data = google.visualization.arrayToDataTable([
          ['Year', 'Sales', 'Expenses'],
          ['2004',  1000,      400],
          ['2005',  1170,      460],
          ['2006',  660,       1120],
          ['2007',  1030,      540]
        ]);

        var options = {
          title: 'Company Performance',
          curveType: 'function',
          legend: { position: 'bottom' },
          'backgroundColor': 'transparent'
        };

        var chart1 = new google.visualization.LineChart(document.getElementById('chart1'));
        var chart2 = new google.visualization.LineChart(document.getElementById('chart2'));
        var chart3 = new google.visualization.LineChart(document.getElementById('chart3'));
        chart1.draw(data, options);
        chart2.draw(data, options);
        chart3.draw(data, options);
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