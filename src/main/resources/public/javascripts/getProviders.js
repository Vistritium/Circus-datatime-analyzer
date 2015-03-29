
$(document).ready(function(){

    $.ajax({
        url: "/providers",
        success: function(providers){
            console.log(providers)

            var toAdd = $('#tiles')

            _.each(providers, function(elem){

                var button = "<a href='events/"+elem+"'><button>"+elem+"</button></a>"

                toAdd.append(button)
            })

        }
    })



})