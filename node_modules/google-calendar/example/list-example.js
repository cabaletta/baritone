var util = require('util');
var express  = require('express');

var config = require('./config');
var gcal = require('../GoogleCalendar');

/*
  ===========================================================================
            Setup express + passportjs server for authentication
  ===========================================================================
*/

var app = express();
var passport = require('passport')
var GoogleStrategy = require('passport-google-oauth').OAuth2Strategy;

app.configure(function() {
  app.use(express.cookieParser());
  app.use(express.bodyParser());
  app.use(express.session({ secret: 'keyboard cat' }));
  app.use(passport.initialize());
});
app.listen(8082);

passport.use(new GoogleStrategy({
    clientID: config.consumer_key,
    clientSecret: config.consumer_secret,
    callbackURL: "http://localhost:8082/auth/callback",
    scope: ['openid', 'email', 'https://www.googleapis.com/auth/calendar'] 
  },
  function(accessToken, refreshToken, profile, done) {
    profile.accessToken = accessToken;
    return done(null, profile);
  }
));

app.get('/auth',
  passport.authenticate('google', { session: false }));

app.get('/auth/callback', 
  passport.authenticate('google', { session: false, failureRedirect: '/login' }),
  function(req, res) { 
    req.session.access_token = req.user.accessToken;
    res.redirect('/');
  });


/*
  ===========================================================================
                               Google Calendar
  ===========================================================================
*/

app.all('/', function(req, res){
  
  if(!req.session.access_token) return res.redirect('/auth');
  
  //Create an instance from accessToken
  var accessToken = req.session.access_token;

  gcal(accessToken).calendarList.list(function(err, data) {
    if(err) return res.send(500,err);
    return res.send(data);
  });
});

app.all('/:calendarId', function(req, res){
  
  if(!req.session.access_token) return res.redirect('/auth');
  
  //Create an instance from accessToken
  var accessToken     = req.session.access_token;
  var calendarId      = req.params.calendarId;
  
  gcal(accessToken).events.list(calendarId, {maxResults:1}, function(err, data) {
    if(err) return res.send(500,err);
    
    console.log(data)
    if(data.nextPageToken){
      gcal(accessToken).events.list(calendarId, {maxResults:1, pageToken:data.nextPageToken}, function(err, data) {
        console.log(data.items)
      })
    }
    
    
    return res.send(data);
  });
});


app.all('/:calendarId/:eventId', function(req, res){
  
  if(!req.session.access_token) return res.redirect('/auth');
  
  //Create an instance from accessToken
  var accessToken     = req.session.access_token;
  var calendarId      = req.params.calendarId;
  var eventId         = req.params.eventId;
  
  gcal(accessToken).events.get(calendarId, eventId, function(err, data) {
    if(err) return res.send(500,err);
    return res.send(data);
  });
});
