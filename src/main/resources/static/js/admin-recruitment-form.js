document.addEventListener('DOMContentLoaded', () => {
    const alwaysOpen = document.querySelector('input[name="alwaysOpen"]');
    const deadline = document.querySelector('input[name="deadline"]');
    const requiredMark = document.getElementById('deadlineRequiredMark');
    const deadlineField = document.getElementById('deadlineField');
    const deadlineInputWrap = document.getElementById('deadlineInputWrap');

    if (!alwaysOpen || !deadline) return;

    const synchronizeDeadline = () => {
        if (alwaysOpen.checked) {
            deadline.value = '';
            deadline.disabled = true;
            deadline.required = false;
            deadline.setAttribute('aria-disabled', 'true');
            deadlineField?.classList.add('is-disabled');
            deadlineInputWrap?.classList.add('is-disabled');
            if (requiredMark) requiredMark.hidden = true;
            return;
        }
        deadline.disabled = false;
        deadline.required = true;
        deadline.removeAttribute('aria-disabled');
        deadlineField?.classList.remove('is-disabled');
        deadlineInputWrap?.classList.remove('is-disabled');
        if (requiredMark) requiredMark.hidden = false;
    };

    alwaysOpen.addEventListener('change', synchronizeDeadline);
    synchronizeDeadline();
});
