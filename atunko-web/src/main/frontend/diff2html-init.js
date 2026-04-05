import { Diff2HtmlUI } from 'diff2html/lib-esm/ui/js/diff2html-ui-slim';

window.atunkoDiff = {
    render: function (elementId, diffString, outputFormat) {
        const element = document.getElementById(elementId);
        if (!element) return;
        const ui = new Diff2HtmlUI(element, diffString, {
            drawFileList: false,
            matching: 'lines',
            outputFormat: outputFormat || 'side-by-side',
            renderNothingWhenEmpty: false,
            diffStyle: 'char',
        });
        ui.draw();
    },
};
