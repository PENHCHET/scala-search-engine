(function(){
  var panes = {"info": false, "settings": false};
  var app = angular.module('search-engine', ['chieffancypants.loadingBar', 'ngAnimate', 'angularUtils.directives.dirPagination']);
  app.config(function(cfpLoadingBarProvider) {
    cfpLoadingBarProvider.includeSpinner = false;
  })
  app.controller('SECtrl', function($scope, $http){
    this.panes = panes;
    this.query = "";
    $scope.results = [];
    $scope.totalResults = 0;
    $scope.resultsPerPage = 10;
    $scope.pagination = {
      current: 1
    };
    $scope.pageChanged = function(newPage) {
      getResultsPage(this.query, newPage);
    };
    function getResultsPage(query, pageNumber) {
      $http.post('/api/search', JSON.stringify({'query': query, 'page': pageNumber}))
      .success(function(data, status, headers, config) {
        //console.log(data)
        $scope.results = data.entities;
        //console.log(data.entities[0])
        $scope.totalResults = data.totalNum;
      })
      .error(function(data, status, headers, config) {
        console.log('Error!');
      });
    }

    this.tooglePane = function(pane){
      this.panes[pane] = !this.panes[pane]
    }
    this.search = function(){
      if(this.query.trim().length > 0){
        getResultsPage(this.query, 1);
      }
    }
    this.startOver = function(){
      this.query = "";
      $scope.results = [];
      $scope.totalResults = 0;
    }
  });
})();
