'use strict';

var _ = require('lodash');
var Policy = require('./TaskRouterCapability').Policy;

var EVENT_URL_BASE = 'https://event-bridge.twilio.com/v1/wschannels';
var TASKROUTER_BASE_URL = 'https://taskrouter.twilio.com';
var TASKROUTER_VERSION = 'v1';

/**
 * Build the default Policies for a worker
 *
 * @param {string} version TaskRouter version
 * @param {string} workspaceSid workspace sid
 * @param {string} workerSid worker sid
 * @returns {Array<Policy>} list of Policies
 */
function defaultWorkerPolicies(version, workspaceSid, workerSid) {
  var activities = new Policy({
    url: _.join([TASKROUTER_BASE_URL, version, 'Workspaces', workspaceSid, 'Activities'], '/'),
    method: 'GET',
    allow: true
  });
  var tasks = new Policy({
    url: _.join([TASKROUTER_BASE_URL, version, 'Workspaces', workspaceSid, 'Tasks', '**'], '/'),
    method: 'GET',
    allow: true
  });
  var reservations = new Policy({
    url: _.join(
      [TASKROUTER_BASE_URL, version, 'Workspaces', workspaceSid, 'Workers', workerSid, 'Reservations', '**'],
      '/'
    ),
    method: 'GET',
    allow: true
  });
  var workerFetch = new Policy({
    url: _.join([TASKROUTER_BASE_URL, version, 'Workspaces', workspaceSid, 'Workers', workerSid], '/'),
    method: 'GET',
    allow: true
  });

  return [activities, tasks, reservations, workerFetch];
}

/**
 * Build the default Event Bridge Policies
 *
 * @param {string} accountSid account sid
 * @param {string} channelId channel id
 * @returns {Array<Policy>} list of Policies
 */
function defaultEventBridgePolicies(accountSid, channelId) {
  var url = _.join([EVENT_URL_BASE, accountSid, channelId], '/');
  return [
    new Policy({
      url: url,
      method: 'GET',
      allow: true
    }),
    new Policy({
      url: url,
      method: 'POST',
      allow: true
    })
  ];
}

/**
 * Generate TaskRouter workspace url
 *
 * @param {string} [workspaceSid] workspace sid or '**' for all workspaces
 * @return {string} generated url
 */
function workspacesUrl(workspaceSid) {
  return _.join(
    _.filter([TASKROUTER_BASE_URL, TASKROUTER_VERSION, 'Workspaces', workspaceSid], _.isString),
    '/'
  );
}

/**
 * Generate TaskRouter task queue url
 *
 * @param {string} workspaceSid workspace sid
 * @param {string} [taskQueueSid] task queue sid or '**' for all task queues
 * @return {string} generated url
 */
function taskQueuesUrl(workspaceSid, taskQueueSid) {
  return _.join(
    _.filter([workspacesUrl(workspaceSid), 'TaskQueues', taskQueueSid], _.isString),
    '/'
  );
}

/**
 * Generate TaskRouter task url
 *
 * @param {string} workspaceSid workspace sid
 * @param {string} [taskSid] task sid or '**' for all tasks
 * @returns {string} generated url
 */
function tasksUrl(workspaceSid, taskSid) {
  return _.join(
    _.filter([workspacesUrl(workspaceSid), 'Tasks', taskSid], _.isString),
    '/'
  );
}

/**
 * Generate TaskRouter activity url
 * 
 * @param {string} workspaceSid workspace sid
 * @param {string} [activitySid] activity sid or '**' for all activities
 * @returns {string} generated url
 */
function activitiesUrl(workspaceSid, activitySid) {
  return _.join(
    _.filter([workspacesUrl(workspaceSid), 'Activities', activitySid], _.isString),
    '/'
  );
}

/**
 * Generate TaskRouter worker url
 *
 * @param {string} workspaceSid workspace sid
 * @param {string} [workerSid] worker sid or '**' for all workers
 * @returns {string} generated url
 */
function workersUrl(workspaceSid, workerSid) {
  return _.join(
    _.filter([workspacesUrl(workspaceSid), 'Workers', workerSid], _.isString),
    '/'
  );
}

/**
 * Generate TaskRouter worker reservation url
 *
 * @param {string} workspaceSid workspace sid
 * @param {string} workerSid worker sid
 * @param {string} [reservationSid] reservation sid or '**' for all reservations
 * @returns {string} generated url
 */
function reservationsUrl(workspaceSid, workerSid, reservationSid) {
  return _.join(
    _.filter([workersUrl(workspaceSid, workerSid), 'Reservations', reservationSid], _.isString),
    '/'
  );
}


module.exports = {
  defaultWorkerPolicies: defaultWorkerPolicies,
  defaultEventBridgePolicies: defaultEventBridgePolicies,

  workspacesUrl: workspacesUrl,
  taskQueuesUrl: taskQueuesUrl,
  tasksUrl: tasksUrl,
  activitiesUrl: activitiesUrl,
  workersUrl: workersUrl,
  reservationsUrl: reservationsUrl
};
