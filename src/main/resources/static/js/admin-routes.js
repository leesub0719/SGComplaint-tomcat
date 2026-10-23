document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('[data-route-create-form]').forEach(function (form) {
        var typeSelect = form.querySelector('[data-route-type]');
        var villageFields = form.querySelector('[data-route-village-fields]');
        var ddokFields = form.querySelector('[data-route-ddok-fields]');
        if (!typeSelect || !villageFields || !ddokFields) return;

        var villageInputs = villageFields.querySelectorAll('input, select, textarea');
        var ddokInputs = ddokFields.querySelectorAll('input, select, textarea');

        function updateFields() {
            var ddokBus = typeSelect.value === 'DDOK';
            villageFields.hidden = ddokBus;
            ddokFields.hidden = !ddokBus;
            villageInputs.forEach(function (input) {
                input.disabled = ddokBus;
                input.required = !ddokBus;
            });
            ddokInputs.forEach(function (input) {
                input.disabled = !ddokBus;
                input.required = ddokBus;
            });
        }

        typeSelect.addEventListener('change', updateFields);
        updateFields();
    });

    document.querySelectorAll('[data-ddok-image-delete]').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            if (!window.confirm('이 똑버스 안내 이미지를 삭제하시겠습니까?')) {
                event.preventDefault();
            }
        });
    });

    document.querySelectorAll('[data-route-delete-form]').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            if (!window.confirm('이 운행안내를 삭제하시겠습니까?')) {
                event.preventDefault();
            }
        });
    });
});
