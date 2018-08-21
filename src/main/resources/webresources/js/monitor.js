
window.onload = init;
function init() {
	$.get('monitorData',function(data) {
		$('#monitors').html(data);
	});
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