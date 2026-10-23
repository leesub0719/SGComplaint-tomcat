document.addEventListener('DOMContentLoaded', () => {
    const appUrl = window.SGComplaint.url;
    const owner = document.querySelector('.detail-owner[data-complaint-no]');
    if (!owner) return;

    const complaintNo = owner.dataset.complaintNo;
    const form = document.getElementById('detail-edit-form');
    const title = document.getElementById('detail-edit-title');
    const category = document.getElementById('detail-edit-category');
    const content = document.getElementById('detail-edit-content');
    const message = document.getElementById('detail-action-message');
    const save = document.getElementById('detail-edit-save');
    const remove = document.getElementById('detail-delete');

    function showError(error) {
        message.textContent = error.message || '요청을 처리하지 못했습니다.';
        message.hidden = false;
    }

    async function send(method, body) {
        const tokenResponse = await fetch(appUrl('/api/csrf'), { credentials: 'same-origin' });
        if (!tokenResponse.ok) throw new Error('보안 토큰을 가져오지 못했습니다. 새로고침 후 다시 시도해 주세요.');
        const csrf = await tokenResponse.json();
        const response = await fetch(appUrl(`/api/mypage/inquiries/${encodeURIComponent(complaintNo)}`), {
            method,
            credentials: 'same-origin',
            headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json',
                [csrf.headerName]: csrf.token,
            },
            ...(body === undefined ? {} : { body: JSON.stringify(body) }),
        });
        const result = (response.headers.get('content-type') || '').includes('application/json')
            ? await response.json() : null;
        if (!response.ok || result?.success === false) {
            throw new Error(result?.message || '요청을 처리하지 못했습니다. 로그인 상태를 확인해 주세요.');
        }
        return result;
    }

    document.getElementById('detail-edit-open').addEventListener('click', () => {
        message.hidden = true;
        form.hidden = false;
        title.focus();
    });

    document.getElementById('detail-edit-cancel').addEventListener('click', () => {
        form.hidden = true;
        message.hidden = true;
    });

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        message.hidden = true;
        const trimmedTitle = title.value.trim();
        if (!trimmedTitle || !content.textContent.trim()) {
            showError(new Error('제목과 내용을 입력해 주세요.'));
            return;
        }
        save.disabled = true;
        try {
            await send('PUT', {
                category: category.value,
                title: trimmedTitle,
                content: content.innerHTML,
            });
            window.location.reload();
        } catch (error) {
            showError(error);
        } finally {
            save.disabled = false;
        }
    });

    remove.addEventListener('click', async () => {
        if (!window.confirm('이 민원을 삭제하시겠습니까? 삭제 후에는 복구할 수 없습니다.')) return;
        message.hidden = true;
        remove.disabled = true;
        try {
            await send('DELETE');
            window.location.assign(appUrl('/complaints'));
        } catch (error) {
            showError(error);
            remove.disabled = false;
        }
    });
});
