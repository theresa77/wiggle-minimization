'use strict';

    angular
        .module('storyline-visualisation')
        .controller('HomeController', HomeController);

    HomeController.$inject = ['$scope', '$q', 'd3Service', '$window'];

    function HomeController ($scope, $q, d3Service, $window) {

        var vm = $scope;
        var endpoint = "/storyline-visualisation-websocket";
        var appDestination = "/storyline-visualisation-app";
        var allStorylines = "/all-storylines";
        var movieDataReceived = "/movie-data";
        var initAllCompressedStorylines = "/initialize";
        var stompClient = null;
        vm.storylines = [];
        vm.compressedStorylines = [];
        vm.currentStoryline = null;
        vm.currentCompressedStoryline = null;
        vm.currentMovieData = null;

        vm.allCompressedStorylines = [];
        vm.selectedStoryline = null;

        vm.colorMap = {
            0: "#CF97D7",
            1: "#FA9D00",
            2: "#ED0026",
            3: "#FEABB9",
            4: "#89DBEC",
            5: "#006884",
            6: "#FFD08D",
            7: "#B00051",
            8: "#00909E",
            9: "#91278F",
            10: "#F68370",
            11: "#688FAD",
            12: "#52CCCE",
            13: "#6E006C"
        };

        $scope.defer = null;

        var connect = function(){
            // console.log("connect() called!");
            $scope.defer = $q.defer();

            var socket = new SockJS(endpoint);
            stompClient = Stomp.over(socket);
            stompClient.connect({}, function (frame) {
                // console.log('Connected: ' + frame);

                stompClient.subscribe(allStorylines, function (visualisation) {
                    // console.log("endpoint "+allStorylines+" called");
                    //list of all saved StorylineDTOs - storylines with movie data
                    var visList = JSON.parse(visualisation.body);
                    angular.forEach(visList, function(vis){
                        vm.allCompressedStorylines.push(vis);
                    });

                    vm.selectedStoryline = vm.allCompressedStorylines[0];

                    vm.createBezierDiagram();
                    $scope.$apply();
                });

                stompClient.subscribe(movieDataReceived, function (movieData) {
                    // console.log("endpoint "+movieDataReceived+" called");
                    vm.currentMovieData = JSON.parse(movieData.body);
                    // console.log(vm.currentMovieData);
                    $scope.$apply();
                });

                $scope.defer.resolve();
            });

        };

        var disconnect =  function(){
            // console.log("disconnect() called!");
            if (stompClient != null) {
                stompClient.disconnect();
            }
            vm.setConnected(false);
            // console.log("Disconnected");
        };

        vm.init = function() {
            // console.log("vm.init() called!");
            stompClient.send(appDestination+initAllCompressedStorylines, {}, {});
        };

        connect();
        $scope.defer.promise.then(function(){
            vm.init();
        });

        vm.createBezierDiagram = function(){

            d3Service.d3().then(function(d3) {
                var bezierLine = d3.svg.line()
                    .x(function (d) { return d[0]; })
                    .y(function (d) { return d[1]; })
                    .interpolate("monotone");

                angular.element( document.querySelector('#storyline')).empty();

                var width = vm.selectedStoryline.storyline[0].length * 30 + 20;
                var height = vm.selectedStoryline.storyline.length * 15 + 12;

                var svg = d3.select("#storyline")
                    .attr("width", width)
                    .attr("height", height);

                //this loop is for drawing the meetings first, behind the character lines as grey rectangles
                for(var i=0; i<vm.selectedStoryline.meetingsInformation.meetingVariables.length; i++){
                    svg.append('rect')
                        .attr("x", vm.selectedStoryline.meetingsInformation.meetingVariables[i].startX - 1)
                        .attr("y", vm.selectedStoryline.meetingsInformation.meetingVariables[i].minY - 6)
                        .attr("width", vm.selectedStoryline.meetingsInformation.meetingVariables[i].endX + 2 - vm.selectedStoryline.meetingsInformation.meetingVariables[i].startX)
                        .attr("height", vm.selectedStoryline.meetingsInformation.meetingVariables[i].maxY + 12 - vm.selectedStoryline.meetingsInformation.meetingVariables[i].minY)
                        .attr('fill','#DDDDDD')
                        .attr('rx', 3)
                        .attr('ry', 3);
                }

                for (var i = 0; i < vm.selectedStoryline.movieData.nodeCount; i++) {
                    var coordinatesList = vm.selectedStoryline.coordinatesMap[i];
                    var strokeWidth = 5;
                    var tpRadius = 3;

                    angular.forEach(coordinatesList, function (coordinates) {
                        if(coordinates.length>1){
                            svg.append('path')
                                .attr("d", bezierLine(coordinates))
                                .attr("stroke", vm.colorMap[i])
                                .attr("stroke-width", strokeWidth)
                                .attr("fill", "none");
                            angular.forEach(coordinates, function (coordinatesCircle) {
                                svg.append('circle')
                                    .attr("cx", coordinatesCircle[0])
                                    .attr("cy", coordinatesCircle[1])
                                    .attr("fill", vm.colorMap[i])
                                    .attr("r", tpRadius);
                            });
                        } else {
                            svg.append('circle')
                                .attr("cx", coordinates[0][0])
                                .attr("cy", coordinates[0][1])
                                .attr("fill", vm.colorMap[i])
                                .attr("r", strokeWidth);
                        }
                    });
                }

            });

            d3Service.d3().then(function(d3) {
                var bezierLine = d3.svg.line()
                    .x(function (d) { return d[0]; })
                    .y(function (d) { return d[1]; })
                    .interpolate("monotone");

                angular.element( document.querySelector('#storyline-big')).empty();
                var svg = d3.select("#storyline-big");

                //this for loop is for drawing the meetings first, behind the character lines as grey rectangles
                for(var i=0; i<vm.selectedStoryline.meetingsInformation.meetingVariables.length; i++){
                    svg.append('rect')
                        .attr("x", vm.selectedStoryline.meetingsInformation.meetingVariables[i].startX - 14)
                        .attr("y", vm.selectedStoryline.meetingsInformation.meetingVariables[i].minY - 22)
                        .attr("width", vm.selectedStoryline.meetingsInformation.meetingVariables[i].endX + 28 - vm.selectedStoryline.meetingsInformation.meetingVariables[i].startX)
                        .attr("height", vm.selectedStoryline.meetingsInformation.meetingVariables[i].maxY + 44 - vm.selectedStoryline.meetingsInformation.meetingVariables[i].minY)
                        .attr('fill','#DDDDDD')
                        .attr('rx', 3)
                        .attr('ry', 3);
                }

                for (var i = 0; i < vm.selectedStoryline.movieData.nodeCount; i++) {
                    var coordinatesList = vm.selectedStoryline.coordinatesMap[i];
                    var strokeWidth = 13;
                    var tpRadius = 8;

                    angular.forEach(coordinatesList, function (coordinates) {
                        if(coordinates.length>1){
                            svg.append('path')
                                .attr("d", bezierLine(coordinates))
                                .attr("stroke", vm.colorMap[i])
                                .attr("stroke-width", strokeWidth)
                                .attr("fill", "none");
                            angular.forEach(coordinates, function (coordinatesCircle) {
                                svg.append('circle')
                                    .attr("cx", coordinatesCircle[0])
                                    .attr("cy", coordinatesCircle[1])
                                    .attr("fill", vm.colorMap[i])
                                    .attr("r", tpRadius);
                            });
                        } else {
                            svg.append('circle')
                                .attr("cx", coordinates[0][0])
                                .attr("cy", coordinates[0][1])
                                .attr("fill", vm.colorMap[i])
                                .attr("r", strokeWidth);
                        }
                    });
                }

            });

            d3Service.d3().then(function(d3) {
                var bezierLine = d3.svg.line()
                    .x(function (d) { return d[0]; })
                    .y(function (d) { return d[1]; })
                    .interpolate("basis");

                var width = vm.selectedStoryline.coordinatesMapUnCompressed[0][0].length * 8 + 20;
                var height = vm.selectedStoryline.storyline.length * 20 + 12;

                // Draw uncompressed storyline
                angular.element(document.querySelector('#storyline-uncompressed')).empty();
                var svg = d3.select("#storyline-uncompressed")
                    .attr("width", width)
                    .attr("height", height);

                //this loop is for drawing the meetings first, behind the character lines as grey rectangles
                for(var i=0; i<vm.selectedStoryline.meetingsInformation.meetingVariables.length; i++){
                    svg.append('rect')
                        .attr("x", vm.selectedStoryline.meetingsInformation.meetingVariables[i].umcomprStartX)
                        .attr("y", vm.selectedStoryline.meetingsInformation.meetingVariables[i].umcomprMinY - 8)
                        .attr("width", vm.selectedStoryline.meetingsInformation.meetingVariables[i].umcomprEndX - vm.selectedStoryline.meetingsInformation.meetingVariables[i].umcomprStartX)
                        .attr("height", vm.selectedStoryline.meetingsInformation.meetingVariables[i].umcomprMaxY + 16 - vm.selectedStoryline.meetingsInformation.meetingVariables[i].umcomprMinY)
                        .attr('fill','#DDDDDD')
                        .attr('rx', 3)
                        .attr('ry', 3);
                }

                var strokeWidth = 6;
                var tpRadius = 4;

                for (var i = 0; i < vm.selectedStoryline.movieData.nodeCount; i++) {
                    var coordinatesList = vm.selectedStoryline.coordinatesMapUnCompressed[i];
                    angular.forEach(coordinatesList, function (coordinates) {
                        //draw each character line as svg path
                        if(coordinates.length>1){
                            svg.append('path')
                                .attr("d", bezierLine(coordinates))
                                .attr("stroke", vm.colorMap[i])
                                .attr("stroke-width", strokeWidth)
                                .attr("fill", "none");

                            svg.append('circle')
                                .attr("cx", coordinates[0][0]+(tpRadius-1))
                                .attr("cy", coordinates[0][1])
                                .attr("fill", vm.colorMap[i])
                                .attr("r", tpRadius);

                            svg.append('circle')
                                .attr("cx", coordinates[coordinates.length-1][0]-(tpRadius-1))
                                .attr("cy", coordinates[coordinates.length-1][1])
                                .attr("fill", vm.colorMap[i])
                                .attr("r", tpRadius);

                        } else {
                            svg.append('circle')
                                .attr("cx", coordinates[0][0])
                                .attr("cy", coordinates[0][1])
                                .attr("fill", vm.colorMap[i])
                                .attr("r", strokeWidth);
                        }
                    });
                }
            });
        };

        vm.downloadStorylineAsPNG = function (imgId, canvasId) {
            console.log('downloadStorylineAsPNG called:');
            console.log(imgId);
            console.log(canvasId);

            var width = 0;
            var height = 0;

            if(imgId === "#storyline"){
                width = vm.selectedStoryline.storyline[0].length * 30 + 20;
                height = vm.selectedStoryline.storyline.length * 15 + 12;
            } else if(imgId === "#storyline-uncompressed") {
                width = vm.selectedStoryline.coordinatesMapUnCompressed[0][0].length * 8 + 20;
                height = vm.selectedStoryline.storyline.length * 20 + 12;
            }

            angular.element(document.querySelector(canvasId)).empty();

            var storylineCanvas = d3.select(canvasId)
                .attr("width", width)
                .attr("height", height);

            var svgStoryline = d3.select(imgId)
                .attr("version", 1.1)
                .attr("xmlns", "http://www.w3.org/2000/svg")
                .node().parentNode.innerHTML;

            var imgsrc = 'data:image/svg+xml;base64,'+ btoa(svgStoryline);

            var canvas = document.querySelector(canvasId);
            var context = canvas.getContext("2d");

            var image = new Image;
            image.src = imgsrc;
            image.onload = function() {
                context.drawImage(image, 0, 0, width, height);
                var canvasdata = canvas.toDataURL("image/png");

                var userAgent = $window.navigator.userAgent;
                console.log('$window.navigator');
                console.log($window.navigator);
                console.log('userAgent');
                console.log(userAgent);

                if(userAgent.indexOf('Firefox') !== -1){
                    // code for download image at firefox or chrome (currently only used for firefox)
                    var event = new MouseEvent('click', {
                        'view': window,
                        'bubbles': true,
                        'cancelable': true
                    });
                    var a = document.createElement('a');
                    a.setAttribute('download', vm.selectedStoryline.movieData.movieName+".png");
                    a.setAttribute('href', canvasdata);
                    a.setAttribute('target', '_blank');
                    a.dispatchEvent(event);

                } else {
                    //code for download image at safari or chrome
                    var a = document.createElement("a");
                    a.download = vm.selectedStoryline.movieData.movieName+".png";
                    a.href = canvasdata;
                    a.click();
                }

            };
        };

        vm.downloadStorylineAsSVG = function (imgId) {
            var svgData = d3.select(imgId)
                .attr("version", 1.1)
                .attr("xmlns", "http://www.w3.org/2000/svg")
                .node().parentNode.innerHTML;
            var svgBlob = new Blob([svgData], {type:"image/svg+xml;charset=utf-8"});
            var svgUrl = URL.createObjectURL(svgBlob);
            var downloadLink = document.createElement("a");
            downloadLink.href = svgUrl;
            downloadLink.download = vm.selectedStoryline.movieData.movieName+".svg";
            document.body.appendChild(downloadLink);
            downloadLink.click();
            document.body.removeChild(downloadLink);
        }

    }

