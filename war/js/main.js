window.hth = {
  init: function(options) {
    this.options = options;
    this.setupBrowseTabs();
    this.animateFlash();
    this.setupDatatables();
    this.setupChosen();
    this.setupCalendars();
    /* for some reason the datepicker doesn't respect the default
    locale immediately */
    window.setTimeout(function() {
      $("#calendar").datepicker("refresh");
    }, 10);
  },
  setupBrowseTabs: function(event) {
    $('#uitabs').tabs({
      selected : 0,
    });
    // for forward and back
    $.address.change(function(event){
      $("#uitabs").tabs( {
        "show": function(event, ui) {
          var oTable = $('div.dataTables_scrollBody>table.tabular', ui.panel).dataTable();
          if ( oTable.length > 0 ) {
            oTable.fnAdjustColumnSizing();
          }
        }
      })
    });
    // when the tab is selected update the url with the hash
    $("#uitabs").bind("tabsselect", function(event, ui) { 
      window.location.hash = ui.tab.hash;
    })
  },
  animateFlash: function() {
    $('#flash').hide()
    $('#flash').fadeIn(1500);
    window.setTimeout("$('#flash').slideUp()", 5000);
  },
  setupDatatables: function() {
    $('.tabular').dataTable( {
      "sScrollY": "300px",
      "bScrollCollapse": true,
      "bPaginate": false,
    });
  },
  setupChosen: function() {
    $('table.chosen select').chosen()
    //$('select').chosen();
  },
  setupCalendars: function() {
    $.datepicker.setDefaults($.datepicker.regional[ "en-GB" ]);
    var dateselector = $('#date, #dob, #secure-tenancy-date, #last-mental, #last-employment, #asylum-date');
    dateselector.datepicker({dateFormat:'dd/mm/yy'});
    dateselector.datepicker( "option", "showAnim", "fadeIn" );
    var calendarselector = $('div#calendar');
    calendarselector.datepicker({
      onSelect: function(dateText, inst) {
        window.location.href = "/calendar/" + dateText;
      }
    });
  }
};
