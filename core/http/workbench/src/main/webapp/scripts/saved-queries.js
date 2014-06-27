/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />
// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.
var workbench;
(function (workbench) {
    (function (savedQueries) {
        function deleteQuery(savedBy, name, urn) {
            var currentUser = workbench.getCookie("server-user");
            if ((!savedBy || currentUser == savedBy)) {
                if (confirm("'" + name + "' will no longer be accessible, even using your browser's history. " + "Do you really wish to delete it?")) {
                    document.forms.namedItem(urn).submit();
                }
            } else {
                alert("'" + name + "' was saved by user '" + savedBy + "'.\nUser '" + currentUser + "' is not allowed do delete it.");
            }
        }
        savedQueries.deleteQuery = deleteQuery;

        function toggleElement(urn, suffix) {
            var htmlElement = document.getElementById(urn + suffix);
            htmlElement.style.display = (htmlElement.style.display == 'none') ? '' : 'none';
        }

        function toggle(urn) {
            toggleElement(urn, '-metadata');
            toggleElement(urn, '-text');
            var toggle = document.getElementById(urn + '-toggle');
            var attr = 'value';
            var show = 'Show';
            var text = toggle.getAttribute(attr) == show ? 'Hide' : show;
            toggle.setAttribute(attr, text);
        }
        savedQueries.toggle = toggle;
    })(workbench.savedQueries || (workbench.savedQueries = {}));
    var savedQueries = workbench.savedQueries;
})(workbench || (workbench = {}));

workbench.addLoad(function () {
    // not using jQuery.html(...) for this since it doesn't do the
    // whitespace correctly
    var queries = document.getElementsByTagName('pre');
    for (var i = 0; i < queries.length; i++) {
        queries[i].innerHTML = queries[i].innerHTML.trim();
    }

    $('[name="edit-query"]').find('[name="query"]').each(function () {
        $(this).attr('value', $(this).attr('value').trim());
    });
});
//# sourceMappingURL=saved-queries.js.map
