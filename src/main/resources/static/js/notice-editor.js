document.addEventListener('DOMContentLoaded', function () {
    var form = document.querySelector('#notice-form');
    if (!form) return;

    var editor = document.querySelector('#notice-editor');
    var shell = document.querySelector('.notice-editor-shell');
    var content = document.querySelector('#notice-content');
    var count = document.querySelector('#notice-content-count');
    var message = document.querySelector('#notice-editor-message');
    var font = document.querySelector('#notice-editor-font');
    var size = document.querySelector('#notice-editor-size');
    var imageButton = document.querySelector('#notice-image-button');
    var imageInput = document.querySelector('#notice-content-images');
    var selectedImages = [];
    var savedRange = null;

    function textValue() {
        return (editor.innerText || '').replace(/\u00a0/g, ' ').trim();
    }

    function syncContent() {
        content.value = editor.innerHTML;
        count.textContent = String(textValue().length);
        shell.classList.remove('invalid');
    }

    function rememberSelection() {
        var selection = window.getSelection();
        if (!selection.rangeCount || !editor.contains(selection.anchorNode)) return;
        savedRange = selection.getRangeAt(0).cloneRange();
    }

    function restoreSelection() {
        editor.focus();
        if (!savedRange) return;
        var selection = window.getSelection();
        selection.removeAllRanges();
        selection.addRange(savedRange);
    }

    function rememberDropPosition(event) {
        var range = null;
        if (document.caretRangeFromPoint) {
            range = document.caretRangeFromPoint(event.clientX, event.clientY);
        } else if (document.caretPositionFromPoint) {
            var position = document.caretPositionFromPoint(event.clientX, event.clientY);
            if (position) {
                range = document.createRange();
                range.setStart(position.offsetNode, position.offset);
                range.collapse(true);
            }
        }
        if (range && editor.contains(range.startContainer)) savedRange = range;
    }

    function runCommand(command, value) {
        restoreSelection();
        document.execCommand(command, false, value || null);
        rememberSelection();
        syncContent();
    }

    function insertNodeAtSelection(node) {
        restoreSelection();
        var selection = window.getSelection();
        if (selection.rangeCount && editor.contains(selection.anchorNode)) {
            var range = selection.getRangeAt(0);
            range.deleteContents();
            range.insertNode(node);
            range.setStartAfter(node);
            range.collapse(true);
            selection.removeAllRanges();
            selection.addRange(range);
            savedRange = range.cloneRange();
        } else {
            editor.appendChild(node);
        }
        editor.appendChild(document.createElement('br'));
        syncContent();
    }

    function addImages(files) {
        var currentImageCount = editor.querySelectorAll('img').length;
        if (currentImageCount + files.length > 5) {
            message.textContent = '본문 이미지는 최대 5장까지 등록할 수 있습니다.';
            message.classList.add('error');
            return;
        }
        for (var index = 0; index < files.length; index++) {
            var file = files[index];
            if (!file.type.startsWith('image/') || file.size > 5 * 1024 * 1024) {
                message.textContent = 'JPG, PNG, GIF, WEBP 형식의 5MB 이하 이미지만 등록할 수 있습니다.';
                message.classList.add('error');
                continue;
            }
            var imageIndex = selectedImages.length;
            selectedImages.push(file);
            var image = document.createElement('img');
            image.src = URL.createObjectURL(file);
            image.alt = file.name;
            image.dataset.imageIndex = String(imageIndex);
            image.addEventListener('click', function (event) {
                editor.querySelectorAll('img').forEach(function (item) { item.classList.remove('selected'); });
                event.currentTarget.classList.add('selected');
            });
            insertNodeAtSelection(image);
        }
        message.textContent = '이미지를 클릭한 후 Delete 키를 누르면 본문에서 제거할 수 있습니다.';
        message.classList.remove('error');
    }

    function prepareImagesForSubmit() {
        var transfer = new DataTransfer();
        var images = Array.from(editor.querySelectorAll('img[data-image-index]'));
        images.forEach(function (image, newIndex) {
            var originalIndex = Number(image.dataset.imageIndex);
            if (!selectedImages[originalIndex]) return;
            transfer.items.add(selectedImages[originalIndex]);
            image.src = 'notice-image:' + newIndex;
            image.removeAttribute('class');
            image.removeAttribute('data-image-index');
        });
        imageInput.files = transfer.files;
        syncContent();
    }

    editor.innerHTML = content.value || '';
    if (editor.dataset.editMode !== 'true') {
        editor.querySelectorAll('img').forEach(function (image) { image.remove(); });
    }
    syncContent();

    editor.addEventListener('input', syncContent);
    editor.addEventListener('keyup', rememberSelection);
    editor.addEventListener('mouseup', rememberSelection);
    editor.addEventListener('paste', function (event) {
        event.preventDefault();
        document.execCommand('insertText', false, event.clipboardData.getData('text/plain'));
    });
    editor.addEventListener('dragenter', function (event) {
        if (!event.dataTransfer || !event.dataTransfer.types.includes('Files')) return;
        event.preventDefault();
        shell.classList.add('drag-active');
    });
    editor.addEventListener('dragover', function (event) {
        if (!event.dataTransfer || !event.dataTransfer.types.includes('Files')) return;
        event.preventDefault();
        event.dataTransfer.dropEffect = 'copy';
        rememberDropPosition(event);
        shell.classList.add('drag-active');
    });
    editor.addEventListener('dragleave', function (event) {
        if (!editor.contains(event.relatedTarget)) shell.classList.remove('drag-active');
    });
    editor.addEventListener('drop', function (event) {
        event.preventDefault();
        shell.classList.remove('drag-active');
        rememberDropPosition(event);
        var files = event.dataTransfer ? Array.from(event.dataTransfer.files) : [];
        if (!files.length) {
            message.textContent = '이미지 파일을 끌어다 놓아 주세요.';
            message.classList.add('error');
            return;
        }
        addImages(files);
    });
    editor.addEventListener('keydown', function (event) {
        if ((event.key === 'Delete' || event.key === 'Backspace')) {
            var selected = editor.querySelector('img.selected');
            if (selected) {
                event.preventDefault();
                selectedImages[Number(selected.dataset.imageIndex)] = null;
                URL.revokeObjectURL(selected.src);
                selected.remove();
                syncContent();
            }
        }
    });
    editor.addEventListener('click', function (event) {
        if (event.target.tagName !== 'IMG') {
            editor.querySelectorAll('img.selected').forEach(function (image) {
                image.classList.remove('selected');
            });
        }
    });

    document.querySelectorAll('[data-notice-command]').forEach(function (button) {
        button.addEventListener('mousedown', function (event) { event.preventDefault(); });
        button.addEventListener('click', function () { runCommand(button.dataset.noticeCommand); });
    });
    font.addEventListener('change', function () { runCommand('fontName', font.value); });
    size.addEventListener('change', function () { runCommand('fontSize', size.value); });
    imageButton.addEventListener('click', function () {
        rememberSelection();
        imageInput.click();
    });
    imageInput.addEventListener('change', function () {
        addImages(Array.from(imageInput.files));
        imageInput.value = '';
    });

    form.addEventListener('submit', function (event) {
        var length = textValue().length;
        if (length === 0 || length > 3000) {
            event.preventDefault();
            shell.classList.add('invalid');
            message.textContent = length === 0
                ? '공지사항 내용을 입력해 주세요.'
                : '공지사항 내용은 3,000자 이내로 입력해 주세요.';
            message.classList.add('error');
            editor.focus();
            return;
        }
        prepareImagesForSubmit();
    });
});
