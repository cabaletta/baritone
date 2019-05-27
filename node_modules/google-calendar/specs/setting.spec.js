var config = global.config = require(__dirname + '/config.js');
var util   = require('util');
var should = require('should');
var async  = require('async');
var needle = require('needle');

var google_calendar   = require(__dirname +'/../GoogleCalendar.js');


function getAccessToken(callback){
  
  var url = 'https://accounts.google.com/o/oauth2/token';
  var content = {
    refresh_token: config.refresh_token,
    client_id:     config.consumer_key,
    client_secret: config.consumer_secret,
    grant_type:'refresh_token',
  }
  
  needle.post(url, content, function(err, resp, body) {
    if(resp.statusCode != 200){
      return callback(body, null, null);
    }  
    
    return callback(null, body.access_token, new Date().getTime() + (body.expires_in-3) * 1000);
  })
}

describe('google_calendar.setting',function() {
  
  var gcal = null;
  before(function(done) {    
    getAccessToken(function(err, access_token) {
      if(err) return done(err);
      gcal = google_calendar(access_token);
      done();
    })
  })
  
  describe('#list()',function() {
    
    it('return the calendar list' , function(done){

      gcal.settings.list(function(err, result) {
        
        should.not.exist(err);
        should.exist(result);
        should.exist(result.items);
        should.exist(result.items.length);
        done();
      })
    })    
  })

})

