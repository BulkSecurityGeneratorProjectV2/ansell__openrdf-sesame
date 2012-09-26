function addLimit() {
	addParam('limit', document.getElementById('limit').value);
}
			
function addParam(name, value) {
	var url = document.location.href;
	var hasParams = (url.indexOf('?') + 1 || url.indexOf(';') + 1);
	var amp = decodeURIComponent('%26');
	var sep =  hasParams ? amp : ';';
	url = url + sep + name + '=' + value;
	if ('true' != getParameter('know_total')) {
		url = url + amp + 'know_total=true';
	} 
	
	document.location.href = url;
}
			
function nextOffset() {
    var offset = getOffset() + getLimit();
    addParam('offset', offset);
}

function previousOffset() {
    var offset = Math.max(0, getOffset() - getLimit());
    addParam('offset', offset);
}
			
function tailAfter(value, split) {
	return value.substring(value.indexOf(split) + 1);
}
			
function getParameter(name) {
	var rval = '';
	var href = document.location.href;
	var elements = tailAfter(tailAfter(href, '?'), ';');
	elements = elements.split(decodeURIComponent('%26'));
	for (var i=0;elements.length-i;i++) {
		var pair = elements[i].split('=');
		if (name != pair[0]) {
			continue;
		}
				
		rval = pair[1];
		// Keep looping. We are interested in the last value.
	}
				
	return rval;
}
			
function getOffset() {
    var offset = getParameter('offset');
    return ('' == offset) ? 0 : parseInt(offset, 10);
}

function correctLimit() {
	var limit = getParameter('limit');
	if ('' == limit) {
		limit = 0;
	}
	
	document.getElementById('limit').value = limit;
}
			
function getLimit() {
	var limit = document.getElementById('limit').value;
	return parseInt(limit, 10);
}
			
var _htmlDom;

if (typeof(document.cookie) == 'undefined') {
	var obj = document.createElementNS('http://www.w3.org/1999/xhtml', 'object');
	obj.width = 0;
	obj.height = 0;
	obj.type = 'text/html';
	obj.data = 'data:text/html;charset=utf-8,%3Cscript%3Eparent._htmlDom%3Ddocument%3C/script%3E';
	document.getElementsByTagName('body')[0].appendChild(obj);
	document.__defineGetter__('cookie', function() { return _htmlDom.cookie; });
	document.__defineSetter__('cookie', function(c) { _htmlDom.cookie = c; });
}
		
function getCookie(name) {
	var cookies = document.cookie.split(";");
	var rval = "";
	var i,cookie,eq,temp;
	for (i=0; i < cookies.length; i++) {
	    cookie = cookies[i];
	    eq = cookie.indexOf("=");
		temp = cookie.substr(0, eq).replace(/^\s+|\s+$/g,"");
		if (name == temp) {
		    rval = unescape(cookie.substr(eq + 1));
		    break;
		}
	}
  				
	return rval;
}
			
window.onload = function() {
	correctLimit();
    var limit = getLimit();
    var nextButton = document.getElementById('nextX');
    var previousButton = document.getElementById('previousX');
   
    // Using RegExp to preserve any localization.
	var buttonWordPattern = /^[A-z]+\s+/
    var buttonNumberPattern = /\d+$/
    var oldNext = nextButton.value;
    var count = parseInt(buttonNumberPattern.exec(oldNext), 10);
    nextButton.value = buttonWordPattern.exec(oldNext) + limit;
    previousButton.value = 
        buttonWordPattern.exec(previousButton.value) + limit;
    if (getOffset() <= 0 || limit <= 0) {
        previousButton.disabled = true;
    }

    if (count < limit || limit <= 0) {
    	nextButton.disabled = true;
    }
			    
    // Modify title to reflect total_result_count cookie
    if (limit > 0) {
    	var h1 = document.getElementById('title_heading');
    	var oldh1 = h1.innerHTML;
    	var h1start = /^.*\(/
		var c_trc = getCookie('total_result_count');
		var total_result_count = 0;
		if (c_trc.length > 0) {
			total_result_count = parseInt(c_trc, 10);
    	}
			    
    	var have_total_count = (total_result_count > 0);
		var offset = getOffset();
		var first = offset + 1;
		var last = offset + limit;
		last = have_total_count ? 
		Math.min(total_result_count, last) : last;
		var newHTML = h1start.exec(oldh1) + first + '-' + last;
		if (have_total_count) { 
			newHTML = newHTML + ' of ' + total_result_count;
		}

		newHTML = newHTML + ')';
		h1.innerHTML = newHTML;
	}
}