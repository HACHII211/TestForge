export async function fetchStream({ url, body, onMessage }) {
    const response = await fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            Authorization: localStorage.getItem('jwt') || ''
        },
        body: JSON.stringify(body)
    });

    const reader = response.body.getReader();
    const decoder = new TextDecoder('utf-8');

    let buffer = '';
    while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        const chunk = decoder.decode(value, { stream: true });
        buffer += chunk;

        const lines = buffer.split('\n');
        buffer = lines.pop(); // 可能还有一部分没读取完整，留着下次用

        for (const line of lines) {
            if (!line.trim()) continue;

            // 清洗掉 data:data: 或 data:
            let cleanedLine = line;
            if (cleanedLine.startsWith('data:data: ')) {
                cleanedLine = cleanedLine.slice('data:data: '.length);
            } else if (cleanedLine.startsWith('data: ')) {
                cleanedLine = cleanedLine.slice('data: '.length);
            }

            try {
                const json = JSON.parse(cleanedLine);
                const delta = json.choices?.[0]?.delta?.content;
                if (delta && typeof onMessage === 'function') {
                    onMessage(delta);
                }
            } catch (e) {
                console.warn('解析失败', cleanedLine);
            }
        }
    }
}
