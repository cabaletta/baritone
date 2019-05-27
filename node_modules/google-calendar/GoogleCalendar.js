module.exports = function(access_token) {
  return new GoogleCalendar(access_token);
}
module.exports.GoogleCalendar = GoogleCalendar;

var util   = require('util');
var needle = require('needle');

function GoogleCalendar(access_token){
  
  this.request  = function(type, path, params, options, body, callback) {  
    
    var url = 'https://www.googleapis.com/calendar/v3'+path+'?access_token='+access_token;
    
    params = params || {}
    options = options || {}
    options.json = true;
    
    type = type.toUpperCase();
    if(body && typeof body !== 'string') body = JSON.stringify(body);
    
    
    for(var k in params){
      url += '&'+encodeURIComponent(k)+'='+ encodeURIComponent(params[k]);
    }
    
    needle.request(type, url, body, options, responseHandler);
    
    function responseHandler(error, response, body) {
      if(error) return callback(error, body);
      if(body.error) return callback(body.error, null);
      return callback(null, body);
    }
  };
  
  this.acl      = new Acl(this.request);
  this.calendarList = new CalendarList(this.request);
  this.calendars = new Calendars(this.request);
  this.events   = new Events(this.request);
  this.freebusy = new Freebusy(this.request);
  this.settings = new Settings(this.request);
}

// Acl
function Acl(request){ this.request = request; }

Acl.prototype.delete = function(calendarId, ruleId, callback) {
  calendarId = encodeURIComponent(calendarId);
  ruleId     = encodeURIComponent(ruleId);

  this.request('DEL', '/calendars/' + calendarId + '/acl/' + ruleId, 
    {}, {}, null, callback);
}

Acl.prototype.get = function(calendarId, ruleId, callback) {
  calendarId = encodeURIComponent(calendarId);
  ruleId     = encodeURIComponent(ruleId);

  this.request('GET', '/calendars/' + calendarId + '/acl/' + ruleId, 
    {}, {}, null, callback);
}

Acl.prototype.insert = function(calendarId, acl, callback) {
  calendarId = encodeURIComponent(calendarId);

  this.request('POST', '/calendars/' + calendarId + '/acl', 
    {}, {}, acl, callback);
}

Acl.prototype.list = function(calendarId, callback) {
  calendarId = encodeURIComponent(calendarId);

  this.request('GET', '/calendars/' + calendarId + '/acl', 
    {}, {}, null, callback);
}

Acl.prototype.update = function(calendarId, ruleId, acl, callback) {
  calendarId = encodeURIComponent(calendarId);
  ruleId     = encodeURIComponent(ruleId);
  
  this.request('PUT', '/calendars/' + calendarId + '/acl/' + ruleId, 
    {}, {}, acl, callback);
}

Acl.prototype.patch = function(calendarId, ruleId, acl, callback) {
  calendarId = encodeURIComponent(calendarId);
  ruleId     = encodeURIComponent(ruleId);
  
  this.request('PATCH', '/calendars/' + calendarId + '/acl/' + ruleId, 
    {}, {}, acl, callback);
}



// CalendarList
function CalendarList(request){ this.request = request; }

CalendarList.prototype.delete = function(calendarId, option, callback) {
  if(!callback){ callback = option; option = {}; }
  calendarId = encodeURIComponent(calendarId);
  this.request('DELETE', '/users/me/calendarList/'+calendarId, option, {}, null, callback);
}

CalendarList.prototype.get = function(calendarId, option, callback) {
  if(!callback){ callback = option; option = {}; }
  calendarId = encodeURIComponent(calendarId);
  this.request('GET', '/users/me/calendarList/'+calendarId, option, {}, null, callback);
}

CalendarList.prototype.insert = function(calendarList, option, callback) {
  if(!callback){ callback = option; option = {} }
  this.request('POST', '/users/me/calendarList', option, {}, calendarList, callback);
}

CalendarList.prototype.list = function(option, callback) {
  if(!callback){ callback = option; option = {} }
  this.request('GET', '/users/me/calendarList', option, {}, null, callback);
}

CalendarList.prototype.update = function(calendarId, calendarList, option, callback) {
  if(!callback){ callback = option; option = {} }
  calendarId = encodeURIComponent(calendarId);
  this.request('PUT', '/users/me/calendarList/'+calendarId, option, {}, calendarList, callback);
}

CalendarList.prototype.patch = function(calendarId, calendarList, option, callback) {
  if(!callback){ callback = option; option = {} }
  calendarId = encodeURIComponent(calendarId);
  this.request('PATCH', '/users/me/calendarList/'+calendarId, option, {}, calendarList, callback);
}


// Calendars
function Calendars(request){ this.request = request; }

Calendars.prototype.clear = function(calendarId, option, callback) {
  if(!callback){ callback = option; option = {}; }
  calendarId = encodeURIComponent(calendarId);
  this.request('POST', '/calendars/'+calendarId+'/clear', option, {}, null, callback);
}

Calendars.prototype.delete = function(calendarId, option, callback) {
  if(!callback){ callback = option; option = {}; }
  calendarId = encodeURIComponent(calendarId);
  this.request('DELETE', '/calendars/'+calendarId, option, {}, null, callback);
}

Calendars.prototype.get = function(calendarId, option, callback) {
  if(!callback){ callback = option; option = {}; }
  calendarId = encodeURIComponent(calendarId);
  this.request('GET', '/calendars/'+calendarId, option, {}, null, callback);
}

Calendars.prototype.insert = function(calendar, option, callback) {
  if(!callback){ callback = option; option = {}; }
  this.request('POST', '/calendars',  option, {}, calendar, callback);
}

Calendars.prototype.update = function(calendarId, calendar, option, callback) {
  if(!callback){ callback = option; option = {}; }
  calendarId = encodeURIComponent(calendarId);
  this.request('PUT', '/calendars/'+calendarId,  option, {}, calendar, callback);
}

Calendars.prototype.patch = function() {
  if(!callback){ callback = option; option = {}; }
  calendarId = encodeURIComponent(calendarId);
  this.request('PATCH', '/calendars/'+calendarId,  option, {}, calendar, callback);
}



// Events
function Events(request){ this.request = request; }


Events.prototype.delete = function(calendarId, eventId, option, callback) {
  
  if(!callback){ callback = option; option = {}; }
  
  calendarId = encodeURIComponent(calendarId);
  eventId    = encodeURIComponent(eventId);
  
  this.request('DELETE', '/calendars/'+calendarId+'/events/'+eventId, 
    option, {}, null, callback);
}

Events.prototype.get = function(calendarId, eventId, option, callback) {
  
  if(!callback){ callback = option; option = {}; }
  
  calendarId = encodeURIComponent(calendarId);
  eventId    = encodeURIComponent(eventId);
  
  this.request('GET', '/calendars/'+calendarId+'/events/'+eventId, 
    option, {}, null, callback);
}

Events.prototype.import = function(calendarId, event, option, callback) {
  
  if(!callback){ callback = option; option = {}; }
  
  calendarId = encodeURIComponent(calendarId);
  
  this.request('POST', '/calendars/'+calendarId+'/events/import', 
    option, {}, event, callback);
}

Events.prototype.insert = function(calendarId, event, option, callback) {
  
  if(!callback){ callback = option; option = {}; }
  
  calendarId = encodeURIComponent(calendarId);
  
  this.request('POST', '/calendars/'+calendarId+'/events', 
    option, {}, event, callback);
}

Events.prototype.instances = function(calendarId, eventId, option, callback) {
  
  if(!callback){ callback = option; option = {}; }
  
  calendarId = encodeURIComponent(calendarId);
  eventId    = encodeURIComponent(eventId);
  
  this.request('GET', '/calendars/'+calendarId+'/events/'+eventId+'/instances', 
    option, {}, null, callback);
}

Events.prototype.list = function(calendarId, option, callback) {
  
  if(!callback){ callback = option; option = {}; }
  
  calendarId = encodeURIComponent(calendarId);
  
  this.request('GET', '/calendars/'+calendarId+'/events', 
    option, {}, null, callback);
}

Events.prototype.move = function(calendarId, eventId, option, callback) {
  
  if(!callback){ callback = option; option = {}; }
  
  calendarId = encodeURIComponent(calendarId);
  eventId    = encodeURIComponent(eventId);
  
  this.request('POST', '/calendars/'+calendarId+'/events/'+eventId+'/move', 
    option, {}, null, callback);
}

Events.prototype.quickAdd = function(calendarId, text, option, callback) {
  
  if(!callback){ callback = option; option = {}; }
  
  option.text = text;
  calendarId = encodeURIComponent(calendarId);
  
  this.request('POST', '/calendars/'+calendarId+'/events/quickAdd', 
    option, {}, null, callback);
}

Events.prototype.update = function(calendarId, eventId, update, option, callback) {
  
  if(!callback){ callback = option; option = {}; }
  
  calendarId = encodeURIComponent(calendarId);
  eventId    = encodeURIComponent(eventId);
  
  this.request('PUT', '/calendars/'+calendarId+'/events/'+eventId, 
    option, {}, update, callback);
}

Events.prototype.patch = function(calendarId, eventId, patch, option, callback) {
  if(!callback){ callback = option; option = {}; }
  
  calendarId = encodeURIComponent(calendarId);
  eventId    = encodeURIComponent(eventId);
  
  this.request('PATCH', '/calendars/'+calendarId+'/events/'+eventId, 
    option, {}, patch, callback);
}

Events.prototype.watch = function(calendarId, watch_request, option, callback){
  if(!callback){ callback = option; option = {}; }
  
  calendarId = encodeURIComponent(calendarId);
  
  this.request('POST', '/calendars/'+calendarId+'/events/watch', 
    option, {}, watch_request, callback);
}

Events.prototype.stopWatch = function(stop_request, callback){

  this.request('POST', '/channels/stop', 
    {}, {}, stop_request, callback);
}


// Freebusy
function Freebusy(request){ this.request = request; }

Freebusy.prototype.query = function(query, option, callback) {
  
  if(!callback){ callback = option; option = {}; }
  this.request('POST', '/freeBusy/', option, {}, query, callback);
}

// Settings
function Settings(request){ this.request = request; }


Settings.prototype.list = function(callback) {

  this.request('GET', '/users/me/settings', 
    {}, {}, null, callback);
}

Settings.prototype.get = function(setting, callback) {
  setting = encodeURIComponent(setting)

  this.request('GET', '/users/me/settings/'+setting, 
    {}, {}, null, callback);
}


