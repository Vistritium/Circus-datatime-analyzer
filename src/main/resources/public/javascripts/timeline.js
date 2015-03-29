//var url = "http://analyze.maciejnowicki.com/getData";
var url = "/getData";

$(document).ready(function () {

    appendNextTimeLine();
    appendNextTimeLine();
    appendNextTimeLine();
    appendNextTimeLine();
    appendNextTimeLine();
    appendNextTimeLine();

    checkAndAppend();

});

var timelines = [];
var failFetchCounter = 0;

var filter = "";

var loadingData = false;


function fetchData(from, to, filter, onDone) {
    var postData = JSON.stringify({
        nameFilter: filter,
        from: from.toISOString(),
        to: to.toISOString(),
        provider: provider
    });

    console.log(postData)

    $.ajax({
        url: url,
        type: "POST",
        contentType: "application/json",
        data: postData,
        success: function (data, textStatus, jqXHR) {
            console.log(data)
            onDone(data);
        },
        error: function (jqxHR, textStatus, errorThrown) {
            console.log(jqxHR);
            console.log(textStatus);
            console.log(errorThrown);
        }
    });
    loadingData = false;
}

function transformToTimetableData(data) {
    var finalData = [];
    var counter = 1;

    _.each(data, function (userObj) {
        var name = userObj.name;
        _.each(userObj.times, function (timeTable) {
            var appear = timeTable.appear;
            var disappear = timeTable.disappear;

            var entry = {
                id: counter,
                content: name,
                start: appear,
                group: name
            };

            var interval = new Date(disappear).getTime() - new Date(appear).getTime();
            if (interval > 1000 * 60 * name.length * 2) {
                entry.end = disappear;
            }

            finalData.push(entry);
            counter = counter + 1;
        });
    });
    return finalData;
}

function createTimeLine(id, from, to, filter) {
    function applyTimeLine(data) {
        // DOM element where the Timeline will be attached
        var container = document.getElementById(id);

        var finalData = transformToTimetableData(data);

        if (finalData.length > 0) {

            // Create a DataSet (allows two way data-binding)
            var items = new vis.DataSet(finalData);

            // Configuration for the Timeline
            var options = {
                align: "left",
                max: to.toISOString(),
                min: from.toISOString(),
                showCurrentTime: true,
                zoomable: false,
                clickToUse: true/*,
                 zoomMax: 1000 * 60 * 60,
                 zoomMin: 100 * 60 * 60 * 24*/
            };

            // Create a Timeline
            var timeline = new vis.Timeline(container, items, options);
            timeline.from = from;
            timeline.to = to;
            timeline.elementId = id;
            timelines.push(timeline)
        } else {
            console.log("failFetchCounter increased")
            failFetchCounter = failFetchCounter + 1;
        }
        checkAndAppend();
    }

    fetchData(from, to, filter, applyTimeLine)
}

function appendNextTimeLine() {
    if(failFetchCounter > 5){
        loadingData = false;
        return false;
    }
    if (!window.counter) {
        window.counter = 1;
    } else {
        window.counter = window.counter + 1;
    }

    var id = "timeLine" + window.counter;
    $('#visualization').append('<div id="{id}"></div>'.supplant({id: id}));

    var from = moment().millisecond(0).minute(0).hour(0).second(1).subtract(window.counter - 2, 'days');
    var to = from.clone().add(1, 'days').subtract(2, 'sec');

    createTimeLine(id, from, to, filter);
    return true;
}


$(window).scroll(function () {

    if ($(window).scrollTop() == $(document).height() - $(window).height()) {
        // ajax call get data from server and append to the div
        appendNextTimeLine();
    }
});

function recreateFilter() {

    _.each(timelines, function (timeline) {
        fetchData(timeline.from, timeline.to, filter, function (data) {
            var finalData = transformToTimetableData(data);
            timeline.setItems(new vis.DataSet(finalData))
            checkAndAppend();
        });
    });
}

window.setInterval(checkAndAppend, 500);

function checkAndAppend(){
    console.log("Checking")

    var heightCondition = !($("body").height() > $(window).height());

    if (heightCondition && (!loadingData)) {
        loadingData = true;
        console.log("Extending");
        appendNextTimeLine();
    } else {
        console.log("Vertical bar exists")
    }
}

function recreateFilter2(filterr){
    filter = filterr;
    recreateFilter();
}

$(window).ready(function(){
    $("#filter").keyup(function (e) {
        if (e.keyCode == 13) {
            var text = $("#filter").val();
            recreateFilter2(text);
        }
    });
});

var provider = function(){
    var pathname = $(location).attr('pathname')

    var splitted = pathname.split('/')

    var provider = splitted[splitted.length - 1]

    return provider;
}()