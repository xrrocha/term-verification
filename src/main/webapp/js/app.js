'use strict';

/* App Module */
angular.module('termVerification', ['ngGrid', 'termVerificationFilters', 'termVerificationServices']).
  config(['$routeProvider', function($routeProvider) {
    $routeProvider.
      when('/', {templateUrl: 'partials/start-screen.html',   controller: StartScreenCtrl}).
      when('/terms', {templateUrl: 'partials/term-verification.html',   controller: TermVerificationCtrl}).
      otherwise({redirectTo: '/'});
  }]);
