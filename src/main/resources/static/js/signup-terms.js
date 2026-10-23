document.addEventListener('DOMContentLoaded', function () {
    var agreeAll = document.querySelector('#agree-all');
    var requiredAgreements = Array.from(
        document.querySelectorAll('.required-agreement'));
    var continueButton = document.querySelector('#continue-signup');

    function updateAgreementState() {
        var allChecked = requiredAgreements.every(function (checkbox) {
            return checkbox.checked;
        });
        agreeAll.checked = allChecked;
        agreeAll.indeterminate = !allChecked && requiredAgreements.some(function (checkbox) {
            return checkbox.checked;
        });
        continueButton.disabled = !allChecked;
    }

    agreeAll.addEventListener('change', function () {
        requiredAgreements.forEach(function (checkbox) {
            checkbox.checked = agreeAll.checked;
        });
        updateAgreementState();
    });

    requiredAgreements.forEach(function (checkbox) {
        checkbox.addEventListener('change', updateAgreementState);
    });

    updateAgreementState();
});
