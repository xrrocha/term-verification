function StartScreenCtrl($scope, $location, $http, options) {
    $scope.options = options;
    
    $scope.changeView = function(view) {
        $location.path(view)
    }
    
    $http.get('api/terms', { params: { query: "lastModified" } }).success(
        function(terms) {
            $scope.recentTerms = terms;
            if (terms.length > 0 && terms[0].termId)
                $scope.options.startTerm = terms[0].termId;
        });
}

function TermVerificationCtrl($scope, $http, options) {
	$scope.terms = [];

	$scope.term = {};
	$scope.names = {};
	$scope.usages = {};
	$scope.similars = {};
    
    var termTypes = {
        "n": "Name",
        "x": "NonName",
        "a": "Abbreviation",
        "t": "Typo",
        "m": "Mixup",
        "o": "Other",
        "u": "Undefined"
    };
    
    $scope.termsBack = function() {
        var params = options.populateParams({
        	end: $scope.terms[0].termId,
        	count: options.pageSize });
        $http.get('api/terms', { params: params }).success(
            function(termData) {
                $scope.terms = termData.filter(function(term) {
                    term.termTypeTitle = termTypes[term.termType];
                    return term;
                });
                selectTerm($scope.terms[0].termId)
            });
    }
    
    $scope.termsForward = function() {
        var params = options.populateParams({
        		start: $scope.terms[$scope.terms.length - 1].termId,
        		count: options.pageSize });
        $http.get('api/terms', { params: params }).success(
            function(termData) {
                $scope.terms = termData.filter(function(term) {
                    term.termTypeTitle = termTypes[term.termType];
                    return term;
                });
                selectTerm($scope.terms[0].termId)
            });
    }

	var params = options.populateParams({
		start: options.startTerm,
		count: options.pageSize });
	$http.get('api/terms', { params: params }).success(
		function(termData) {
			$scope.terms = termData.filter(function(term) {
				term.termTypeTitle = termTypes[term.termType];
			    return term;
			});
			selectTerm($scope.terms[0].termId)
		});

	var usageTypes = [
	    { usageCode: "f", title: "Female Name" },
	    { usageCode: "m", title: "Male Name" },
        { usageCode: "s", title: "Surname" },
        { usageCode: "F", title: "Female Middle" },
        { usageCode: "M", title: "Male Middle" }
	];

	function selectTerm(termId) {
		var termUrl = 'api/terms/' + termId; 
		$http.get(termUrl).success(function(termData) {
			for (p in termData) {
				$scope[p] = termData[p]
			}

			var termDataUsageTypes = {};
			termData.usages.forEach(function(usage) {
			    termDataUsageTypes[usage.usage] = usage.baseTermId
			});
			
			$scope.usages = usageTypes.map(function(usage) {
				if (usage.usageCode in termDataUsageTypes) {
					return {
						usageCode: usage.usageCode,
						title: usage.title,
						used: true,
						baseTermId: termDataUsageTypes[usage.usageCode]
					}
				} else {
					return {
						usageCode: usage.usageCode,
						title: usage.title,
						used: false,
						baseTermId: ""
					}
				}
			});
		});
	}
    
    $scope.saveChanges = function() {
        var data = {
            term: {
                termId: $scope.term.termId,
                termType: $scope.term.termType,
                baseTermId: $scope.term.baseTermid
            },
            usages: []
        };
        
        //$scope.selections[0].termType = $scope.term.termType;
        
        if ($scope.term.termType == "n") {
            var usedUsage = function(usage) { return usage.used; }
            var setupUsage = function(usage) {
                var result = {}
                result.usage = usage.usageCode
                if (usage.baseTermId) {
                    var baseTermId = usage.baseTermId.replace(/^\s+|\s+$/g, "")
                    if (baseTermId != "") {
                        result.baseTermId = baseTermId
                    }
                }
                return result;
            }
            data.usages = $scope.usages.filter(usedUsage).map(setupUsage);
        }
        
        if ($scope.term.termType == "a") {
        	var baseTermId = $scope.term.baseTermId.replace(/^\s+|\s+$/g, "");
        	if (baseTermId == "") {
        		alert("Abbreviation requires base term");
        		return;
        	}
        	data.term.baseTermId = baseTermId;
        }
        
        if ($scope.term.termType == "t") {
        	var baseTermId = $scope.term.baseTermId.replace(/^\s+|\s+$/g, "");
        	if (baseTermId == "") {
        		alert("Typo requires base term");
        		return;
        	}
        	data.term.baseTermId = baseTermId;
        }

        var termUrl = 'api/terms/' + $scope.term.termId; 
        $http.put(termUrl, data).error(function(response) {
            console.log("Error PUT /" + termUrl + ": " + response);
        });
        
        // Synchronize term grid with new term type
        for (i in $scope.terms) {
        	if ($scope.terms[i].termId == $scope.term.termId) {
        		$scope.terms[i].termType = $scope.term.termType; 
        		$scope.terms[i].termTypeTitle = termTypes[$scope.term.termType];
        		break;
        	}
        }
    };

	$scope.termColumnDefs = [
	    { field: 'termId', displayName: 'Term', groupable: false },
	    { field: 'termTypeTitle', displayName: 'Type', groupable: false }
	];
	$scope.termGridOptions = {
		data: 'terms',
		canSelectRows: true,
		multiSelect: false,
		displayFooter: false,
		columnDefs: 'termColumnDefs',
		//selectedItems: $scope.selections,
		displaySelectionCheckbox: false,
		showColumnMenu: false,
		beforeSelectionChange: function(rowItem) {
		    // TODO Return false if pending changes (add reset button)
			var term = rowItem.entity
			selectTerm(term.termId);
			return true;
		}
	};

	$scope.nameColumnDefs = [
	    { field: 'name', displayName: 'Names' }
	];
	$scope.nameGridOptions = {
		data: 'names',
		multiSelect: false,
		columnDefs: 'nameColumnDefs',
		footerVisible: false,
		displaySelectionCheckbox: false,
		showColumnMenu: false
	}

	$scope.similarColumnDefs = [
	    { field: "termId", displayName: "Similar Terms" }
	];
    $scope.similarGridOptions = {
		data: 'similarities',
		columnDefs: 'similarColumnDefs',
		multiSelect: false,
		footerVisible: false,
		displaySelectionCheckbox: false,
		showColumnMenu: false,
		beforeSelectionChange: function(rowItem) {
			var similar = rowItem.entity
		    $http.get('api/terms/' + similar.termId + "/names").success(
		            function(names) {
		                $scope.similarNames = names;
		            });
			return true;
		}
    }
    
    $scope.similarNames = []
    $scope.similarNameColumnDefs = [
        { field: "name", displayName: "Similar Names" }
    ];
    $scope.similarNameGridOptions = {
         data: 'similarNames',
         columnDefs: 'similarNameColumnDefs',
         multiSelect: false,
         footerVisible: false,
         displaySelectionCheckbox: false,
         showColumnMenu: false
    };
}

function googleItUp(termId) {
    url = "http://google.com?q=" + termId + "&lr=es&cr=co&gl=co&pws=0&hl=es";
    console.log(url);
    window.open(url, "_blank", "height=1000,width=1000", false)
}

function focus(id) {
    document.getElementById(id).focus();
}
