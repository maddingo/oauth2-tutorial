'use client';

import { useAuth } from '@/context/AuthContext';
import { getQuote } from "@/lib/quote-api";
import { useEffect, useState } from "react";

export default function Quote() {
    const { isAuthenticated, refreshAccessToken } = useAuth();
    const [quote, setQuote] = useState<{quote: string, author: string} | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState<boolean>(false);

    const fetchQuote = async () => {
        setLoading(true);
        setError(null);

        try {
            const quoteData = await getQuote();
            setQuote(quoteData);
        } catch (err) {
            console.error('Failed to fetch quote:', err);

            // If token expired, try refreshing and retry
            if (err instanceof Error && err.message === 'TOKEN_EXPIRED') {
                try {
                    console.log('Token expired, attempting refresh...');
                    await refreshAccessToken();
                    // Retry fetching quote after refresh
                    const quoteData = await getQuote();
                    setQuote(quoteData);
                } catch (refreshErr) {
                    console.error('Failed to refresh token:', refreshErr);
                    setError('Session expired. Please log in again.');
                }
            } else {
                setError(err instanceof Error ? err.message : 'Failed to fetch quote');
            }
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (isAuthenticated) {
            fetchQuote();
        }
    }, [isAuthenticated]);

    if (error) {
        return <div className="error">Error loading quote: {error}</div>;
    }

    if (loading || !quote) {
        return <div>Loading quote...</div>;
    }

    return (
        <div>
            <div className="quote">{quote.quote}</div>
            <div className="author">{quote.author}</div>
        </div>
    );
}
