
'use strict';

var storylineVisualisationApp = angular
        .module('storyline-visualisation', [
            'pascalprecht.translate',
            'ngResource',
            'ngAria',
            'ui.bootstrap',
            'angular-loading-bar',
            'ngRoute',
            'btford.socket-io',
            'nvd3ChartDirectives',
            'd3'])
    .config(function($routeProvider) {

        $routeProvider
            .when('/', {
                templateUrl : 'app/home/home.html',
                controller : 'HomeController'
            })
            .when('/home', {
                templateUrl : 'app/home/home.html',
                controller : 'HomeController'
            })
            .otherwise({ redirectTo: '/' });
    })
    .constant('storyline-visualisation', 'http://localhost:8080/');

storylineVisualisationApp.run(['$rootScope',
        function($rootScope) {

    }]);
