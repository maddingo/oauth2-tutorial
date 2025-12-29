'use client';

import { useAuth } from '@/context/AuthContext';
import { getQuote } from "@/lib/quote-api";
import { useEffect, useState } from "react";

export default function Quote() {
    const { isAuthenticated } = useAuth();
    const [quote, setQuote] = useState<{quote: string, author: string} | null>(null);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (isAuthenticated) {
            getQuote()
                .then(setQuote)
                .catch(err => {
                    console.error('Failed to fetch quote:', err);
                    setError(err.message);
                });
        }
    }, [isAuthenticated]);

    if (error) {
        return <div className="error">Error loading quote: {error}</div>;
    }

    if (!quote) {
        return <div>Loading quote...</div>;
    }

    return (
        <div>
            <div className="quote">{quote.quote}</div>
            <div className="author">{quote.author}</div>
        </div>
    );
}
