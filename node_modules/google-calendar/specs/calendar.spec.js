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

describe('google_calendar.calendar',function() {
  
  var gcal = null;
  before(function(done) {    
    getAccessToken(function(err, access_token) {
      if(err) return done(err);
      gcal = google_calendar(access_token);
      done();
    })
  })
  
  describe('#get()',function() {
    
    it('return the calendar' , function(done){

      gcal.calendarList.list(function(err, _result) {

        should.not.exist(err);
        should.exist(_result);
        should.exist(_result.items[0]);

        gcal.calendars.get(_result.items[0].id,function(err, result) {
  
          should.not.exist(err);
          should.exist(result);
          result.id.should.equal(_result.items[0].id)
          result.kind.should.equal('calendar#calendar')
          done()
        })
      })
    })
  })


  describe('insert and delete',function() {
    
    var inserted_calendar_id = null;
    var calendar = { summary: 'Test' }

    it('return create a new calendar and delete it' , function(done){
      gcal.calendars.insert(calendar, function(err, result) {
        
        should.not.exist(err);
        should.exist(result);
        inserted_calendar_id = result.id
        
        gcal.calendars.delete(inserted_calendar_id, function(err, result) {
          should.not.exist(err);
          should.exist(result);
          
          done()
        })
      })
    })
    
  })

})

