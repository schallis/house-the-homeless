window.hth = {
  init: function(options) {
    this.options = options;
    //this.setupFavourites();
    //this.setupFollowing();
    //this.setupDeletion();
    //this.setupDragUpload();
    this.setupBrowseTabs();
    this.animateFlash();
    this.setupDatatables();
  },
  // setupFavourites: function(event) {
  //   var self = this;
  //   $('#favourite-form').submit(function() {
  //     var form = $(this);
  //     var btn = form.find('button');
  //     var url = btn.hasClass('favourite') ? self.options.urls.removeFavourite
  //                                         : self.options.urls.addFavourite;
  //     $.ajax({
  //       type: 'POST',
  //       url: url,
  //       data: {picture: form.find('input[name=picture]').val()},
  //       dataType: 'json',
  //       success: function(response) {
  //         if (response.success) {
  //           if (btn.hasClass('favourite')) {
  //             btn.removeClass('favourite');
  //             btn.html(self.options.strings.addFavourite);
  //           } else {
  //             btn.addClass('favourite');
  //             btn.html(self.options.strings.removeFavourite);
  //           }
  //         }
  //       }
  //     });
  //     return false;
  //   });
  // },
  // setupFollowing: function(event) {
  //   var self = this;
  //   $('#follow-form').submit(function() {
  //     var form = $(this);
  //     var btn = form.find('button');
  //     var url = btn.hasClass('following') ? self.options.urls.unfollow
  //                                         : self.options.urls.follow;
  //     $.ajax({
  //       type: 'POST',
  //       url: url,
  //       data: {user: form.find('input[name=user]').val()},
  //       dataType: 'json',
  //       success: function(response) {
  //         if (response.success) {
  //           if (btn.hasClass('following')) {
  //             btn.removeClass('following');
  //             btn.html(self.options.strings.follow);
  //           } else {
  //             btn.addClass('following');
  //             btn.html(self.options.strings.unfollow);
  //           }
  //         }
  //       }
  //     });
  //     return false;
  //   });
  // },
  //   setupDeletion: function(event) {
  //   var self = this
  //   $('#delete-initiate').click(function() {
  //     var initiate = $(this)
  //     var confirm_form = $('#delete-complete');
  //     initiate.addClass('hidden')
  //     confirm_form.removeClass('hidden')
  //     return false;
  //   });
  // },
  // setupDragUpload: function(event) {
  //       var drop = document.getElementById('upload-drop-target');
  //       var drop_msg = document.getElementById('upload-drop-target-message')
  //       // Check for browser support
  //       if (typeof window.FileReader === 'undefined') {
  //           drop_msg.innerHTML = 'It looks like your browser does not support drag and drop upload. You should think about upgrading to a more modern browser :)';
  //           drop_msg.className = 'warning';
  //       } else {
  //           drop_msg.className = 'info';
  //       }
  //       // Handle drag states
  //       drop.ondragover = function () { this.className = 'hover'; return false; };
  //       drop.ondragend = function () { this.className = 'dropped'; return false;};
  //       drop.ondrop = function (e) {
  //         var file = e.dataTransfer.files[0];
  //         e.preventDefault();
  //         this.className = 'dropped';
  //         drop_msg.innerHTML = 'Uploading ' + file.name;
  //         drop_msg.className = 'warning';
  //         //Ajax upload image
  //         var xhr = new XMLHttpRequest();
  //         xhr.onreadystatechange = function() {
  //           if (xhr.readyState == 4) {
  //             if ((xhr.status >= 200 && xhr.status <= 200) || xhr.status == 304) {
  //               var response = $.parseJSON(xhr.responseText)
  //               if (response.success) {
  //                   drop_msg.innerHTML = 'Success!';
  //                   drop_msg.className = 'success';
  //               }
  //             }
  //           }
  //         }
  //         // Show file in browser
  //         reader = new FileReader();
  //         reader.onload = function (event) {
  //           drop.style.background = 'url(' + event.target.result + ') no-repeat center';
  //           xhr.open("POST", '/dragupload/', true); // open asynchronous post request
  //           xhr.setRequestHeader("X-Requested-With", "XMLHttpRequest");
  //           xhr.send(event.target.result);
  //           console.log(file);
  //         };
  //         reader.readAsDataURL(file);
  //         return false;
  //       };

  // },
  setupBrowseTabs: function(event) {
    $('#uitabs').tabs({
        selected : 0,
    });
    // for forward and back
    $.address.change(function(event){
      $("#uitabs").tabs( "select" , window.location.hash )
      })
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
    $('.tabular').dataTable();
  }
};
