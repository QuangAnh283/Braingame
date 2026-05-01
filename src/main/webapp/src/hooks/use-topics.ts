import { useState, useEffect, useCallback } from 'react';
import topicApi from '../services/topic-api';

export const useTopics = () => {
    const [topics, setTopics] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const loadTopics = useCallback(async () => {
        try {
            setLoading(true);
            setError('');
            const result = await topicApi.getAll();

            if (result.success && Array.isArray(result.data)) {
                setTopics(result.data);
            } else {
                setTopics([]);
                setError(result.error || 'Không thể tải danh sách chủ đề');
            }
        } catch {
            setError('Không thể tải danh sách chủ đề');
            setTopics([]);
        } finally {
            setLoading(false);
        }
    }, []);

    const getTopicById = (topicId) => {
        return topics.find(topic => topic.id.toString() === topicId.toString());
    };

    const searchTopics = async (query) => {
        try {
            const result = await topicApi.search(query);
            return result.success && Array.isArray(result.data) ? result.data : [];
        } catch {
            return [];
        }
    };

    const searchTopicsPaginated = async (keyword = '', page = 0, size = 10, sort = 'id,desc') => {
        try {
            const result = await topicApi.searchPaginated(keyword, page, size, sort);
            return result.success ? result.data : { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 };
        } catch {
            return { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 };
        }
    };

    const refreshTopics = () => {
        loadTopics();
    };

    useEffect(() => {
        loadTopics();
    }, [loadTopics]);

    return {
        topics,
        loading,
        error,
        getTopicById,
        searchTopics,
        searchTopicsPaginated,
        refreshTopics,
        setError
    };
};

export default useTopics;