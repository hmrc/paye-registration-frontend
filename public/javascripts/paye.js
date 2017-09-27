$(document).ready($(function() {

    changeHrefForFirefox()
    function changeHrefForFirefox() {
        var isFirefox = typeof InstallTrigger !== 'undefined';
        if (isFirefox) {
            $(".error-summary-list a").each(function (index, object) {
                var element = $(object).attr("data-focuses");

                // Matches 1 or more characters at the start of the string followed by [ followed by one or more numbers, followed by ]
                var regex = /^\w+\[\d+\]$/;
                if(regex.test(element) || element.indexOf('.') > -1){
                    $(object).prop("href", "javascript:document.getElementById('" + element + "').focus()")
                }
            })
        }
    }

        $(".error-summary #description-error-summary").click(function(){
        $("#description").focus()
    })

    var director = $("#completionCapacity-director");
    var agent = $("#completionCapacity-agent");
    var other = $("#completionCapacity-other");
    var otherHidden = $("#other-hidden");

    if(other.is(":checked")){
        otherHidden.show();
    } else {
        otherHidden.hide();
    }

    director.on("change", function () {
        otherHidden.hide();
    });

    agent.on("change", function () {
        otherHidden.hide()
    });

    other.on("change", function () {
        otherHidden.show();
    });

    $('*[data-hidden]').each(function() {

        var $self = $(this);
        var $hidden = $('#hidden')
        var $input = $self.find('input');

        if ($input.val() === 'true' && $input.prop('checked')) {
            $hidden.show();
        } else {
            $hidden.hide();
        }

        $input.change(function() {

            var $this = $(this);

            if ($this.val() === 'true') {
                $hidden.show();
            } else if($this.val() === 'false') {
                $hidden.hide();
            }
        });
    });

    var radioOptions = $('input[type="radio"]');

    radioOptions.each(function() {
        var o = $(this).parent().next('.additional-option-block');
        if ($(this).prop('checked')) {
            o.show();
        } else {
            o.hide();
        }
    });

    radioOptions.on('click', function(e){
        var o = $(this).parent().next('.additional-option-block');
        if(o.index() == 1){
            $('.additional-option-block').hide();
            o.show();
        }
    });
}));