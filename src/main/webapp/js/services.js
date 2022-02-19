'use strict';

angular.module('termVerificationServices', []).
    service('options', function() {
    	var termTypes = {
			name: true,
			nonName: true,
			abbreviation: true,
			typo: true,
			mixup: true,
			other: true,
			undef: true
    	};
    	return {
    		pageSize: 15,
    		startTerm: "a",
    		termTypes: termTypes,
	    	populateParams: function(params) {
	    		var result = {};
	    		for (var optionName in termTypes) {
	    			result[optionName] = termTypes[optionName];
	    		}
	    		for (var paramName in params) {
	    			result[paramName] = params[paramName];
	    		}
	    		return result;
	    	}
    	};
    });
