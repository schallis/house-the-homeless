window.hth = {
    init: function(options) {
        this.options = options;
        this.setupBrowseTabs();
        this.animateFlash();
        this.setupDatatables();
    },
    setupBrowseTabs: function(event) {
        $('#uitabs').tabs({
            selected : 0,
        });
        // for forward and back
        $.address.change(function(event){
            $("#uitabs").tabs( 
                "select", window.location.hash
            )
        });
        // when the tab is selected update the url with the hash
        $("#uitabs").bind("tabsselect", function(event, ui) { 
            window.location.hash = ui.tab.hash;
        })
    },
    animateFlash: function() {
        var flash = $('#flash');
        window.setTimeout("$('#flash').slideUp()", 5000);
    },
    setupDatatables: function() {
        $('.tabular').dataTable( {
            "bJQueryUI": true,
        });
    }
};
