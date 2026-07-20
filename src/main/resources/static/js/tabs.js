// Chuyển tab trang bài học — JS vanilla, vendored (AD-5: không CDN).
// No-JS: mọi panel hiện sẵn (progressive enhancement); JS bật thì chỉ hiện panel active.
(function () {
    var root = document.querySelector('[data-tabs]');
    if (!root) return;
    var tabs = Array.prototype.slice.call(root.querySelectorAll('[data-tab]'));
    var panels = Array.prototype.slice.call(document.querySelectorAll('[data-panel]'));
    if (!tabs.length) return;

    function activate(name) {
        tabs.forEach(function (t) {
            t.classList.toggle('on', t.getAttribute('data-tab') === name);
        });
        panels.forEach(function (p) {
            p.hidden = p.getAttribute('data-panel') !== name;
        });
    }

    tabs.forEach(function (t) {
        t.addEventListener('click', function () {
            activate(t.getAttribute('data-tab'));
        });
    });

    // Mặc định: tab đầu tiên (tab đầu có nội dung do server render)
    activate(tabs[0].getAttribute('data-tab'));
}());
