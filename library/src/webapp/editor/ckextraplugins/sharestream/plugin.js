

//var CKEDITOR = CKEDITOR || {};
var ckeditorId;
(function() {
    var pluginName = 'sharestream';
    CKEDITOR.plugins.add(pluginName,
        {
            init: function(editor) {

                ckeditorId = editor.name;
                var height = 480;
                var width = 770;
                CKEDITOR.dialog.addIframe(pluginName,
                    'sharestream',
                    '/sharestream-media-tool/ckeditor.htm?userid=' + parent.portal.user.id + '&siteid=' + parent.portal.siteId+'&mode='+editor.name,
                    width,
                    height,
                    function() {
                        var ckDialog = CKEDITOR.dialog.getCurrent();
                        document.getElementById(ckDialog.getButton('ok').domId).style.display='none';
                        document.getElementById(ckDialog.getButton('cancel').domId).style.display='none';
                    },

                    {
                        onOk : function() {}
                    }
                );
                editor.addCommand(pluginName, new CKEDITOR.dialogCommand( 'sharestream' ) );

                editor.ui.addButton(pluginName, {
                    label: pluginName,
                    command: pluginName,
                    icon: this.path + 'images/sharestream.png'
                });
            }
        }
    );

})();
