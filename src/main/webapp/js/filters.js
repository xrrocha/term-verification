'use strict';

angular.module('termVerificationFilters', []).filter('showHideLabel', function() {
  return function(shown) {
    if (shown)
    	return "Hide";
    else
    	return "Show";
  };
});
